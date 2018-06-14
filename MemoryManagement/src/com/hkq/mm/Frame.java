package com.hkq.mm;

/**
 * 页框类
 */
public class Frame {
	
	public int frameNum;		// 页框号
	public boolean used;		// 标志该页框是否使用
	public int beginAddress;	// 该页框起始地址
	
	public String id;			// 该页框现在被那个进程使用，不是页框必须的信息，只是为了展示内存时更直观
	
	public Frame(int frameNum, int beginAddress) {
		super();
		this.frameNum = frameNum;
		this.beginAddress = beginAddress;
		setUnused();
	}
	
	public void setUsed(String id) {
		this.used = true;
		this.id = id;
	}
	
	public void setUnused() {
		this.used = false;
		this.id = null;
	}
}
