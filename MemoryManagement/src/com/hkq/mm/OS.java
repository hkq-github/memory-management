package com.hkq.mm;

import java.util.Map;
import java.util.HashMap;

public class OS {
	public static final int memorySize = 64 * 1024;		// 内存大小
	public static final int pageSize = 1 * 1024;		// 页大小，为了简化地址转换，为2的幂
	public static final int maxSegmentNum = 4;			// 一个程序最多有多少个段
	public static final int maxSegmentSize = 16 * 1024;	// 一个段最大大小
	public static final int maxResidentSetNum = 8;		// 进程驻留集最多多少个页
	
	public static enum REPLACE_POLICY {FIFO, LRU};		// 置换策略 FIFO or LRU
	public static REPLACE_POLICY ReplacePolicy = REPLACE_POLICY.LRU;	// 置换策略，默认为LRU
	
	private Map<String, PCB> processes = new HashMap<>();
	private Memory memory = new Memory(memorySize / pageSize);
	
	public OS() {
		PCB.setMemory(memory);
	}
	
	/**
	 * 创建一个进程，返回创建是否成功
	 */
	public boolean createProcess(String id, int[] segments) {
		String mess = validate(id, segments);
		if(mess != null) {
			System.out.println("创建进程失败(" + mess + ")");
			return false;
		}
		// 验证是否有足够的内存
		PCB process = new PCB(id, segments, ReplacePolicy);
		if(process.residentSetCount > memory.unusedFrameCount()) {
			System.out.println("创建进程失败(内存不足)");
			return false;
		}
		// 申请内存，设置驻留集
		processes.put(id, process);
		int[] frame = memory.mallocFrame(id, process.residentSetCount);
		process.residentSet = frame;
		// 随机载入一些页
		process.initLoad();
		
		System.out.println("创建进程 " + id + " 成功");
		return true;   
	}
	
	/**
	 * 验证创建的进程合法性，若合法，返回null；否则返回错误信息
	 */
	private String validate(String id, int[] segments) {
		if(processes.containsKey(id)) {
			return "进程名重复";
		}
		if(segments.length == 0 || segments.length > 4) {
			return "一个进程只能有1 到 " + OS.maxSegmentNum + " 个段";
		}
		for(int i = 0; i < segments.length; i++) {
			if(segments[i] <= 0 || segments[i] > OS.maxSegmentSize) {
				return "一个段必须小于 " + OS.maxSegmentSize + "KB";
			}
		}
		
		return null; // 合法，返回null
	}
	
	/**
	 * 销毁一个进程
	 */
	public void destroyProcess(String id) {
		PCB process = processes.get(id);
		if(process == null) {
			System.out.println("操作失败，进程" + id + "不存在");
			return ;
		}
		
		int[] frames = process.residentSet;
		memory.freeFrame(frames);
		processes.remove(id);
		System.out.println("销毁进程" + id + "成功");
	}
	
	/**
	 * 将逻辑地址（段号+段偏移）转化成物理地址。若出错，返回-1
	 * 如果发生缺页中断，根据置换策略选择一个页换出，并将请求的页载入到内存中
	 */
	public int toPhysicalAddress(String id, int segmentNum, int segmentOffset) {
		PCB process = processes.get(id);
		if(process == null) {
			System.out.println("操作失败，进程" + id + "不存在");
			return -1;
		}
		// 判断请求的段是否存在
		if (segmentNum < 0 || segmentNum >= process.STable.length) {
			System.out.println("操作失败，进程" + id + " 段(" + segmentNum + ")不存在");
			return -1;
		}
		
		SegmentEntry segment = process.STable[segmentNum];
		// 若段偏移大于段大小，则请求失败
		if(segmentOffset > segment.segmentSize) {
			System.out.println("操作失败，进程 " + id + " 段偏移(" + segmentOffset +") 越界");
			return -1;
		}
		
		// 根据segmentOffset计算页号和页偏移
		int pageNum = segmentOffset / OS.pageSize;
		int pageOffset = segmentOffset % OS.pageSize;
		
		PageEntry page = segment.PTable[pageNum];
		if(page.load == false) {	
			// 如果该页不在内存中，根据淘汰策略淘汰一个页面，并将该页载入
			System.out.println("请求的页不再内存中，发生缺页中断");
			process.replacePage(segmentNum, pageNum);
		}
		
		// 计算物理地址、设置该页使用时间
		page.usedTime = System.currentTimeMillis();
		int frameNum = page.frameNum;
		int beginAddress = memory.getFrame(frameNum).beginAddress;
		System.out.println("进程" + id + "段(" + segmentNum +") 段偏移(" + segmentOffset + ") 物理地址为：" + (beginAddress + pageOffset));
		return beginAddress + pageOffset;
	}
	
	/**
	 * 设置默认置换策略
	 */
	public static void setReplacePolicy(OS.REPLACE_POLICY policy) {
		OS.ReplacePolicy = policy;
	}
	
	/**
	 * 返回内存使用情况
	 */
	public void showMemory() {
		System.out.println(memory.toString());
		System.out.println();
	}
	
	/**
	 * 返回进程的段表和页表
	 */
	public void showProcess(String id) {
		PCB process = processes.get(id);
		if(process == null) {
			System.out.println("要查看的进程不存在");
			return ;
		}
		
		StringBuilder sb = new StringBuilder();
		
		int[] frames = process.residentSet;
		sb.append("驻留集：[ ");
		for(int elem : frames) {
			sb.append(elem + " ");
		}
		sb.append("]\n");
		sb.append("置换策略：");
		if(process.policy == REPLACE_POLICY.FIFO) {
			sb.append("FIFO ");
			sb.append("[ ");
			for(Integer[] something : process.loadQueue) {
				sb.append("(" + something[0] + ", " + something[1] + ") ");
			}
			sb.append("]\n\n");
		} else {
			sb.append("LRU\n\n");
		}
		
		for(SegmentEntry segment : process.STable) {
			sb.append("进程" + id + " 段号:" + segment.segmentNum + " 段大小:" + segment.segmentSize + "\n");
			sb.append("-----------------------------------------------------------------\n");
			sb.append("| 页号\t| 是否载入\t| 页框号\t| 页框起始地址\t| 上一次访问时间\t|\n");
			sb.append("-----------------------------------------------------------------\n");
			for(PageEntry page : segment.PTable) {
				sb.append("| " + page.pageNum + "\t");
				if(page.load) {
					sb.append("| load\t\t| " + page.frameNum + "\t| " + memory.getFrame(page.frameNum).beginAddress + "\t\t| " + page.usedTime + " |\n");
				} else {
					sb.append("| unload\t| \t| \t\t| \t\t|\n");
				}
			}
			sb.append("-----------------------------------------------------------------\n\n");
		}
		
		System.out.print(sb.toString());
	}
}
