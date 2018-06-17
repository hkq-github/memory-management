## 段页式虚拟存储管理

模拟操作系统中的段页式虚拟内存管理。

### 题目描述：
* 内存大小64K，页框大小为1K，一个进程最多有4个段，且每个段最大为16K。一个进程驻留集最多为8页。
* 驻留集置换策略：局部策略（仅在进程的驻留集中选择一页）
* 页面淘汰策略：FIFO、LRU

**要实现的功能**
1. 创建进程：输入进程名、每个段大小
2. 销毁进程：回收该进程占有的内存
3. 查看一个进程当前的驻留集、页面淘汰策略（FIFO or LRU）、段表、页表
4. 查看内存的使用情况
5. 地址映射：请求一个逻辑地址（段号、段偏移），判断是否缺页，如果不缺页，计算输出其物理地址；若缺页，根据驻留集置换策略，选择一页换出并将请求的页换入，再计算其物理地址。

### 程序运行展示
* 创建进程
```
>>> create process P1 1255 3333 2222
IO: 将进程 P1 段(0) 页(0) 读入页框 0 中
IO: 将进程 P1 段(0) 页(1) 读入页框 1 中
IO: 将进程 P1 段(1) 页(0) 读入页框 2 中
IO: 将进程 P1 段(1) 页(1) 读入页框 3 中
IO: 将进程 P1 段(1) 页(2) 读入页框 4 中
IO: 将进程 P1 段(1) 页(3) 读入页框 5 中
IO: 将进程 P1 段(2) 页(0) 读入页框 6 中
IO: 将进程 P1 段(2) 页(1) 读入页框 7 中
创建进程 P1 成功
```
### 详细设计

**一些策略：**
* 放置策略：决定一个进程驻留集存放在内存什么地方。优先存放在低页框。当一个进程进入时，从低页框选择未使用的页框。
* 初始载入策略：创建进程后，决定最开始将那些页载入内存，从第0个、第1个段...依次载入页，直到驻留集已全部载入
#### 一些类的说明：
###### Memory内存模拟类：
由于是模拟，内存可以看作是Frame页框的数组。提供了两个方法：
* 申请内存：`int[] mallocFrame(String id, int n)` 返回申请到的页框的页框号数组。根据放置策略（优先选择低页框），从数组下标0处遍历数组，选择没有被占用的页框。
```java
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
```

* 释放内存：`void freeFrame(int[] frames)` 释放frames数组中页框号的内存。
###### PCB类：
* `void initLoad()` 函数中体现了初始载入策略（从第0个、第1个段...依次载入页，直到驻留集已全部载入）。
```java
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
```
* 当发生缺页中断后，需要根据置换策略（FIFO or LRU）选择一个页换出内存，并将另一个页载入。`void replacePage(int inSN, int inPN)` 代码如下：
```java
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
```
* FIFO的实现`Integer[] selectReplacePage_FIFO()` 
在PCB类中维护一个队列，记录进程中页（段号、页号）的载入顺序，每当页载入内存时入队；要将页换出时，返回队头元素。
```java
// 页载入内存的顺序。其中Integer数组元素分别为段号、页号
// 用于实现替换策略的 FIFO
// 重要：每当页载入内存时更新队列
public Queue<Integer[]> loadQueue = new LinkedList<>();	
```
```java
/**
 * 根据FIFO策略选择一个页，依次返回该页的段号、页号
 */
private Integer[] selectReplacePage_FIFO() {
    return loadQueue.poll();
}
```
* LRU的实现`Integer[] selectReplacePage_LRU()`
在PageEntry页表项类中有`usedTime`变量，记录该页上一次被访问的时间。当该页被初始载入或被访问时，重置时间；要将页换出时，遍历进程所有页，选出usedTime最小的页，返回段号、页号。
```java
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
```
### 功能实现（OS类）：

* **创建进程：** `boolean createProcess(String id, int[] segments)`

  检验创建进程的合法性（进程名是否重复、段的个数、每个段大小是否合法）
  
  创建PCB对象
  
  调用`mallocFrame(String id, intn)`申请内存。若内存不足，提示创建失败，返回
  
  调用`initLoad()`初始载入一些页
  
```java
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
```
  
* **将逻辑地址映射为物理地址：** `int toPhysicalAddress(String id, int segmentNum, int segmentOffset)`

  检验（进程、段是否存在；访问地址是否越界）
  
  根据段偏移计算页号、页偏移
  
  判断访问的页是否存在，不存在调用`replacePage(int inSN, intinPN)` ，选择一页换出内存，并将请求的页载入
  
  计算物理地址，重置该页使用时间
```java
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
```
  
* **销毁进程：** `void destroyProcess(String id)`
* **查看进程：** `void showMemory()`
* **查看内存：** `void showMemory()`
