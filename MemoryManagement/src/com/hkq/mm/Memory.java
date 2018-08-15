package com.hkq.mm;

/**
 * 模拟内存类
 */

public class Memory {
	private Frame[] memory;
	private int unusedFrameCount = 0;
	
	/**
	 * 创建有frameNum个未使用页框的内存
	 */
	public Memory(int frameNum) {
		memory = new Frame[frameNum];
		for(int i = 0; i < frameNum; i++) {
			memory[i] = new Frame(i, i * OS.pageSize);
		}
		unusedFrameCount = frameNum;
	}
	
	/**
	 * 放置策略：优先放在低页框
	 * 申请(设置used为true)前n个未使用的页框，返回包含页框号的数组；若剩余内存不够，返回null
	 */
	public int[] mallocFrame(String id, int n) {
		if(unusedFrameCount < n) {
			return null;
		}
		
		int[] result = new int[n];
		int index = 0;
		for(int i = 0; index < n && i < memory.length; i++) {
			if(memory[i].used == false) {
				result[index] = memory[i].frameNum;
				memory[i].setUsed(id);
				index++;
			}
		}
		unusedFrameCount -= n;
		return result;
	}
	
	/**
	 * 释放页框号为frames的所有页框
	 */
	public void freeFrame(int[] frames) {
		for(int i = 0; i < frames.length; i++) {
			memory[frames[i]].setUnused();
		}
		unusedFrameCount += frames.length;
	}
	
	/**
	 * 未使用页框个数
	 */
	public int unusedFrameCount() {
		return unusedFrameCount;
	}
	
	/**
	 * 模拟从外存读入一页。从外存读入id进程segmentNum段pageNum页frameNum的页框中
	 */
	public void readPage(String id, int segmentNum, int pageNum, int frameNum) {
		System.out.println("IO: 将进程 " + id + " 段(" + segmentNum + ") 页(" + pageNum + ") 读入页框 " + frameNum + " 中");
	}
	/**
	 * 模拟将程序的一页写入外存。将frameNum的内容写入外存，写入内容为id进程segmentNum段pageNum页
	 */
	public void writePage(String id, int segmentNum, int pageNum, int frameNum) {
		System.out.println("IO: 将页框" + frameNum + "内容写入外存。进程 " + id + " 段(" + segmentNum + ") 页(" + pageNum +")");
	}
	
	/**
	 * 返回页框号为frame的页框，若请求页框不存在，返回null
	 */
	public Frame getFrame(int frameNum) {
		if(frameNum >= 0 && frameNum < memory.length) {
			return memory[frameNum];
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("内存使用情况：");
		for(int i = 0; i < memory.length; i++) {
			if(i % 8 == 0) {	// 一行显示8个页框
				sb.append("\n" + i + "-" + (i + 7) + ":\t| ");
			}
			if(memory[i].used) {
				// 截取进程id前5个字符输出
				String id = memory[i].id;
				if(id.length() > 5) {
					id = id.substring(0, 4);
				}
				sb.append(id + "\t| ");
			} else {
				sb.append("     \t| ");
			}
		}
		
		return sb.toString();
	}
}
