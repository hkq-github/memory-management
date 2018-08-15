package com.hkq.mm;

import java.util.Queue;
import java.util.LinkedList;

public class PCB {
	
	private static Memory memory =  null;
	
	public String id;
	public SegmentEntry[] STable;
	public int residentSetCount;			// 驻留集页个数
	public int[] residentSet;				// 驻留集页框号
	public OS.REPLACE_POLICY policy;		// 替换策略
	
	// 页载入内存的顺序。其中Integer数组元素分别为段号、页号
	// 用于实现替换策略的 FIFO
	// 重要：每当页载入内存时更新队列
	public Queue<Integer[]> loadQueue = new LinkedList<>();	
	
	
	public PCB(String id, int[] segments, OS.REPLACE_POLICY policy) {
		this.id = id;
		this.policy = policy;
		STable = new SegmentEntry[segments.length];
		for(int i = 0; i < STable.length; i++) {
			STable[i] = new SegmentEntry(i, segments[i]);
		}
		
		// 计算驻留集大小
		residentSetCount = 0;
		for(int i = 0; i < STable.length; i++) {
			residentSetCount += STable[i].PTable.length;
		}
		if(residentSetCount > OS.maxResidentSetNum) {
			residentSetCount = OS.maxResidentSetNum;
		}
	}
	
	/** 
	 * 只需在调用PCB类方法之前调用一次，用于设置Memory对象
	 */
	public static void setMemory(Memory m) {
		memory = m;
	}
	
	/**
	 * 创建进程完成后，载入一些页。若该程序可以全部放入驻留集中，则将全部程序载入
	 * 初始载入策略：从第0个、第1个段...依次载入页，直到驻留集已全部载入
	 */
	public void initLoad() {
		int index = 0;
		for(SegmentEntry segment : STable) {
			for(PageEntry page : segment.PTable) {
				if(index >= residentSetCount) {
					break;
				}
				page.setLoad(residentSet[index]);
				loadQueue.add(new Integer[]{segment.segmentNum, page.pageNum});
				memory.readPage(id, segment.segmentNum, page.pageNum, residentSet[index]);
				index++;
			}
		}
	}
	
	/* 置换策略：FIFO or LRU */
	/**
	 * 根据FIFO策略选择一个页，依次返回该页的段号、页号
	 */
	private Integer[] selectReplacePage_FIFO() {
		return loadQueue.poll();
	}
	/**
	 * 根据LRU策略选择一个页，依次返回该页的段号、页号
	 */
	private Integer[] selectReplacePage_LRU() {
		long leastTime = System.currentTimeMillis() + 1000000;	// 设置为现在以后的时间
		int segmentNum = -1;
		int pageNum = -1;
		
		// 遍历所有页，找到usedTime最小的
		for(SegmentEntry segment : STable) {
			for(PageEntry page : segment.PTable) {
				if(page.load && page.usedTime < leastTime) {
					leastTime = page.usedTime;
					segmentNum = segment.segmentNum;
					pageNum = page.pageNum;
				}
			}
		}
		
		return new Integer[] {segmentNum, pageNum};
	}
	
	/**
	 * 依据policy策略选择一页换出驻留集，并将segmentNum段pagNum页换入
	 * SN SegmentNum
	 * PN PageNum
	 */
	public void replacePage(int inSN, int inPN) {
		Integer[] something;
		if(policy == OS.REPLACE_POLICY.FIFO) {
			something = selectReplacePage_FIFO();
		} else {
			something = selectReplacePage_LRU();
		}
			
		int outSN = something[0];
		int outPN = something[1];
		
		PageEntry inPage = STable[inSN].PTable[inPN];
		PageEntry outPage = STable[outSN].PTable[outPN];
		int frameNum = outPage.frameNum;
		memory.writePage(id, outSN, outPN, frameNum);
		outPage.setUnload();
		memory.readPage(id, inSN, inPN, frameNum);
		inPage.setLoad(frameNum);
		loadQueue.add(new Integer[]{inSN, inPN});
	}
}

/* Segment Table Entry */
class SegmentEntry {
	public int segmentNum = -1;			// 段号
	public int segmentSize = -1;		// 段大小
	public PageEntry[] PTable = null;			// 对应的页表
	
	public SegmentEntry(int segmentNum, int segmentSize) {
		this.segmentNum = segmentNum;
		this.segmentSize = segmentSize;
		
		int count = segmentSize / OS.pageSize;	// 页表的大小
		if(segmentSize % OS.pageSize != 0) {
			count++;
		}
		PTable = new PageEntry[count];
		for(int i = 0; i < count; i++) {
			PTable[i] = new PageEntry(i);
		}
	}
}

/* Page Table Entry */
class PageEntry {
	public int pageNum;					// 页号
	public boolean load;				// 该页是否载入
	public int frameNum;				// 该页载入的页框号。如果load为false，则该字段无意义
	/**
	 * 该页最近一次被访问的时间。如果load为false，则此字段无意义。
	 * 该字段用于实现页面置换策略LRU，当该页被载入内存或被访问时，重置该时间
	 */
	public long usedTime;				
	public String info = "";			// 其他信息，如设置保护、共享等
	
	/**
	 * 创建一个指定页号、未载入的页
	 */
	public PageEntry(int pageNum) {
		this.pageNum = pageNum;
		setUnload();
	}
	
	/**
	 * 设置该页载入到页框号为frameNum的页框中
	 */
	public void setLoad(int frameNum) {
		this.load = true;
		this.frameNum = frameNum;
		usedTime = System.currentTimeMillis();
	}
	
	/**
	 * 将该页载出内存
	 */
	public void setUnload() {
		this.load = false;
		this.frameNum = -1;
		usedTime = -1;
	}
}
