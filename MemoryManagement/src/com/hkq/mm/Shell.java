package com.hkq.mm;

import java.util.Scanner;

public class Shell {
	public static final String helpMess = 
			"create process 进程id 各个段大小\t创建一个进程\n" + 
			"destroy process 进程id\t\t销毁一个进程\n" +
			"show memory\t\t\t显示内存使用情况\n" +
			"show process 进程id\t\t显示该进程驻留集、置换策略、段表、页表\n" +
			"address 进程名 段号 段偏移\t\t将逻辑地址映射为物理地址\n" +
			"help or h\t\t\t获取帮助\n" +
			"quit or q\t\t\t退出\n";
	
	public static void main(String[] args) {
		printMessage();
		setReplacePolicy();
		System.out.println("输入help获取更多帮助信息");
		shell();
		Input.close();
	}
	
	/**
	 * 1. create process pname segments
	 * 2. destroy process pname
	 * 3. show memory
	 * 4. show process pname
	 * 5. help or h
	 * 6. quit or q
	 * 8. address pname sgementNum segmentOffset
	 */
	public static void shell() {
		OS os = new OS();
		System.out.print(">>> ");
		while(true) {
			String command = Input.nextLine();
			if(command == null || command.trim().equals("")) {
				System.out.print(">>> ");
				continue;
			}
			
			String[] words = command.split(" ");
			if(words.length >= 4 && "create".equals(words[0].trim()) && "process".equals(words[1].trim())) {
				String processId = words[2].trim();
				int[] segments = new int[words.length - 3];
				try{
					for(int i = 3, index = 0; i < words.length; i++, index++) {
						segments[index] = Integer.parseInt(words[i]);
						if(segments[index] <= 0) {
							throw new Exception();
						}
					}
				} catch (Exception ex) {
					System.out.println("命令有误 段大小必须为正整数(help获取帮助)");
					System.out.print(">>> ");
					continue;
				}
				os.createProcess(processId, segments);
				
			} else if(words.length == 3 && "destroy".equals(words[0].trim()) && "process".equals(words[1].trim()) ) {
				String processId = words[2].trim();
				os.destroyProcess(processId);
				
				
			} else if(words.length == 2 && "show".equals(words[0].trim()) && "memory".equals(words[1].trim()) ) {
				os.showMemory();
				
				
			} else if(words.length == 3 && "show".equals(words[0].trim()) && "process".equals(words[1].trim()) ) {
				String processId = words[2].trim();
				os.showProcess(processId);
				
			} else if(words.length == 1 && "help".equals(words[0].trim()) || "h".equals(words[0].trim()) ) {
				System.out.println(helpMess);
				
			} else if(words.length == 1 && "quit".equals(words[0].trim()) || "q".equals(words[0].trim()) ) {
				System.out.println("quit");
				break;
			} else if(words.length == 4 && "address".equals(words[0].trim())) {
				String porcessId = words[1].trim();
				int segmentNum, segmentOffset;
				try{
					segmentNum = Integer.parseInt(words[2].trim());
					segmentOffset = Integer.parseInt(words[3].trim());
					if(segmentNum < 0 || segmentOffset < 0) {
						throw new Exception();
					}
				} catch (Exception ex) {
					System.out.println("命令有误 段号和段偏移必须为正整数(help获取帮助)");
					System.out.print(">>> ");
					continue;
				}
				os.toPhysicalAddress(porcessId, segmentNum, segmentOffset);
				
			} else {
				System.out.println("命令有误(help获取帮助)");
			}
			
			System.out.print(">>> ");
		}
	}
	
	/**
	 * 设置默认置换策略
	 */
	public static void setReplacePolicy() {
		System.out.print(">>> 请设置置换策略(0为FIFO 1为LRU): ");
		while(true) {
			String mess = Input.nextLine().trim();
			if("0".equals(mess)) {
				OS.setReplacePolicy(OS.REPLACE_POLICY.FIFO);
				System.out.println("设置置换策略为FIFO");
				break;
			} else if("1".equals(mess)) {
				OS.setReplacePolicy(OS.REPLACE_POLICY.LRU);
				System.out.println("设置置换策略为LRU");
				break;
			} else {
				System.out.print(">>> 输入有误，请设置置换策略(0为FIFO 1为LRU): ");
			}
		}
	}
	
	/**
	 * 打印一些必要信息
	 */
	public static void printMessage() {
		String version = "1.0";
		
		System.out.println("Memory Management [version " + version + "]");
		System.out.println("Author: hkq");
		System.out.println();
		System.out.println("内存大小64K，页框大小为1K，一个进程最多有4个段，且每个段最大为16K。一个进程驻留集最多为8页。");
		System.out.println("驻留集置换策略：局部策略（仅在进程的驻留集中选择一页）");
		System.out.println("页面淘汰策略：FIFO、LRU");
		System.out.println("进程初始载入策略：从第0个、第1个段...依次载入页，直到驻留集已全部载入");
		System.out.println("放置策略：决定一个进程驻留集存放在内存什么地方。优先放在低页框");
		System.out.println();
		
	}
}

/**
 * 当有多个Scanner对象时，调用close()方法，会顺便关闭System.in对象，如果调用了其他Scanner对象的方法，会出现java.util.NoSuchElementException异常<br/>
 * 
 * 此类是对Scanner的简单封装，用于统一获取输入和关闭Scanner对象
 */
class Input {
	private static Scanner input = new Scanner(System.in);
	
	public static void close() {
		input.close();
	}
	
	public static String nextLine() {
		return input.nextLine();
	}
}