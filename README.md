# myDFS
自研myDFS，渐进式地仿照HDFS，FastDFS等产品手写分布式文件系统

## 健壮记录：
- 当下的功能：
  - 简单上传
    tracker接受存储请求，分配storages给client，client直接给storage传输文件
    上传实则是追加写
    - 用双工形式 / pipeline 提高响应时间 | Raft算法 实现强一致性
    - Raft的问题在于写操作全部流向leader，可能造成瓶颈
  - Raft 解决集群一致性的问题，但要求每个group的写入口只能有一台设备

- 改操作：
  - > 出于性能考虑，避免直接使用windows文件管理，
    - 追加写和直接改：
      - 追加写：判断大小是否小于最后一个分片的空余部分，如果不小于就直接在新分片中写
        - 也要处理并发写问题，并发写的内容虽然只需要前后排开，但需要考虑并发写后是否超过服务器最大空间
            处理方式：写时复制 | 但需要让下一个用户知道还有没有足够大的空间来写 - 变量temp记录排队人数，计算并发即将占用的空间
          - 什么时候合并归一 —— 当线程池中可用线程多起来的时候，并且这个判断写在上传文件的方法中，这样做在没有并发时会一个一个稳定上传，在有并发时会在后面几个触发
            要注意在归一的时候给暂存list和排队数加锁
      - 直接改：
        > java的File读取是基于Stream的，但提供了RandomAccessFile可以直接操作底层
        - 在一个slice内的修改
          - 对于存储满的分片改大必然会溢出，改小会在末尾空出空间
            - 解决方法1：标记追加写方法 —— 追加写，但内容是改某一部分  |  解决新的问题——追加写少内容太多次，导致空余内容太多，定期垃圾回收
            - 解决方法2：链式
              对于更改文件且超过原本空间的：
              将newFIle前4MB写入一个Slice，且设置next为自身，剩下每4MB的存入下一个slice，且设置next为刚刚的next
            - > 具体实现是：整个大文件转化成inputStream流，前(4MB-2bit)读取并存储完后，接着读(4MB-6bit),只有当读满(4MB-6bit)并且输入流中还有数据时，将此
                slice的next设置为自身，nextIndex设置为下一个，否则说明此slice就是整个文件的最后一个
            - --- 问题：怎么优雅的读出4MB-6byte？
                  我只能不优雅的先循环读取4MB-1024Byte，再读一次1018Byte
            - --- 问题：需要明确在storage中怎么找到对应的slice --- 根据hash在List<String>中找--类似索引
            - 并发改怎么办？
                - 在每个storage（JVM）上我给每个slice分配了一把锁，并发写同一个slice的操作自然排队
                  而分布式锁使用的场景是：多个线程修改一个资源，通过在第三方管理中争夺锁（redisson,zookeeper）来获取权限，但我的程序中storage本身就算第三方
      - ```问题记录：怎么保证修改过程中不触及元数据？
        
        ```
        - 跨越多个slice的修改 - 就是跨越多个storage的修改
          分布式事务
          - 思路1：不考虑分布式事务的原子性，就算某一部分写入失败，其他部分即使写成功也不撤回 --> 对于修改操作不行
            - 思路2：配置一台控制服务器，用于2PC，3PC监控提交 --> 
            > 直接改的流程是：client通过tracker查询到目标文件的位置，分别查出来由client拼接成大文件后呈现给用户，用户分别修改每个slice，将修改的部分提交回去
              storage将修改的slice替换原来的slice，并且有可能扩展几个slice
            - 很明显client作为协调者，监控多个文件的替换是否成功 -- 2PC
              - 问题：写入文件怎么能 执行但不提交？
                1. 思路1：充分利用每个slice的validIndex —— 新的要替代的数据在末尾正常写入，假如commit则将原本的slice无效化（validIndex = 0），
                   末尾的新slice代替旧slice，假如要回滚则把新slice的validIndex设置为0； 
                   **好处是**：把宕机的风险压缩到切换一个指针的时间内，和数据库中commit功效类似，下面的写时复制就无法做到这样
                   **问题在于**：要解决storage中间和后面validIndex == 0的slice，后面好说直接改startIndex，中间的只能放弃吗？
                   又一问题在于：要改旧slice上一个slice的头部信息，要求slice的元数据里要有上一个的location和index
                   又一问题在于：改动上一个slice的头部信息也涉及分布式事务，也需要client调度
                   又一问题在于：假如修改范围包括第一片slice，那tracker中对于链式的头节点的记录就要更新了，client缓存也要更新
                   所以更新client缓存的时间是：
                   1. 上传成功
                   2. 修改涉及头节点（怎么区分谁是头节点？元数据中上一个节点location和index为-1）
                2. 思路2：MVCC
                3. @Transactional注解
                    这个事务管理是基于数据库的，有时还要绑定dataSource，并不能解决文件写回滚问题
                4. 还得是 | 写时复制 |
                   相比于第一种思路，慢在选择commit后还要等待复制，但并不会产生莫名的无效空间
                   ```有问题：
                   思路：对于大于4MB的文件，先只把4MB-6byte后面的部分写完；前面部分等待协调者的指令，如果commit，复制副本进入主体，如果rollback，调回startIndex
                   问题：commit的时间太长了，在commit中发生宕机的概率很大，导致2PC没有意义
                   ```
                   应该在prepare阶段为commit失败的风险兜底 -- 还得是先写到副本里，如果宕机的话就不会清理写到副本里的，于是重启就可以用于恢复
                   ```
                   思路：对于大于4MB的文件，先执行事务操作 -- 将4MB-6byte后面的部分写完，同样也需要将4MB-6byte的数据持久化了（跟在最后面），
                   如果commit，将在最后面的slice复制进入主体，startIndex前移屏蔽掉它，如果rollback，startIndex前移屏蔽掉所有
                   对于小于4MB的文件，也是先在最后持久化写完
                   ```
                   其中使用到的技术：子线程对外传递结果 -- 异步任务提交 -- future类 + callable接口
                   但宕机重启后读取文件恢复的程序没有写
                   
     - 分片大小设置：
       - 暂不明
     - **分片结构设置：**
         1. 有下一个分片的位置 1byte
         2. 有下一个分片的index（所在storage的具体偏移量）1byte
         3. 有有效信息占分片的长度 4byte
       整体 6byte
        > 两个都由上层tracker做吧
  - 所以在哪里将大文件分片并且添加标头？数据不经过tracker，就在client
    > client将大文件分成4*1024*1024-6，并为每个slice添加标头----需要在请求tracker之后得到目标storages和对应的index
      然后分别将slices分配到各个storage的leader，leader再按照raft完成一致性
      过程中，tracker中有每个slice的storage和index，|以及每个大文件对应的slices| ~~~~ 
      很矛盾的一点是：tracker要避免对文件的任何操作--计算哈希/分片都交给client，但tracker又需要分片对应关系+哈希
      在向tracker找文件时，正是通过hash找到头节点
    - tracker怎么知道哪些storages是空余的，每个storage下一个存到哪里？
    - storage每隔一定时间发起心跳证明自己存活，在心跳中传递必要信息：storageId，startIndex
      心跳信息：
      ```JSON
      {
        storageId:**,
        startIndex:**,
      }
    - ```
    - 怎么检测storage是否宕机？消息队列的设计是：生产者一发送，生产者就可以被动接受（监听器模式），这样可以按照心跳的频率更新storage的上次心跳时间
      但是遍历检查storage有无心跳（上次心跳时间是否太久远）需要轮询吗？
      方案1：每次接受到心跳时遍历一遍，只要有一台机器存活就可以检测出其他机器宕机
      方案2：**又一线程轮询（和心跳同频，稍慢）**
    - tracker持久化文件信息（内存镜像文件） 文件对应关系
    - > 替代tracker持久化的方式：记录编辑日志 —— 实时写入每个修改文件的操作（事务操作），无论tracker何时宕机，甚至不需要内存镜像文件也能恢复
        但只能 | 配合使用 | --> 镜像文件每一个月更新一次，编辑日志记录此一个月的编辑过程
    - client 缓存tracker中文件对应关系，但当对应关系变动时触发刷新 --- 会不会刷新太频繁？
- 又一思路：链表化 / tracker只存储头节点的位置，接下来每个节点都内含下一个节点所在位置
  - 解决了直接改的弊端：可以直接在后面加一个4MB
  - client的缓存刷新也不必太过频繁
  - 链表化的话，垃圾回收是必要的
  - 缺点：
    - 改操作时怎么能直接修改更改的那部分？难道还要从头根据偏移量找？ | 解决方法是：更改时通过链式找到slice时，直接记录每个分片的位置
    - 每个slice的元数据听起来好用，实现起来略显复杂
      1. 存储服务器对修改后的大文件负责分片+添加头部信息
      2. 确保头部信息不会被修改 -- 在前端修改过程中要映射好修改的文件和实际存储文件的关系 | 前端的文件由一系列不到4MB的slice拼起来...
- 对于tracker
  - 测试元数据大小----tracker压力
  - 信息都在内存中，这就导致宕机危害很大，策略是**优先记录日志，其次更改内存** -- 但我的DFS未必需要这样 -- metaData的数据不常更改且备份在client的缓存中，宕机也无妨
    - 最极端的情况：client向tracker发起登记信息，没登记完tracker宕机 -- 也无妨，用消息确认（返回true）保证成功登记
  - 策略是：client先向tracker请求到一系列可用的storage序号，提交成功后，再向tracker发起记录信息

  - 元数据内容配置：
    1. 文件名称
    2. 整个大文件的第一个slice所在storage，以及偏移量index -- 由于链表化后会在中间增加slice，所以没有存下每个slice来
    潜在配置：文件大小，大文件哈希...但构建的思路是元数据尽量少修改，所以长变的不计入
    ```bytes
    ConcurrentHashMap<Sting,Integer[2]> -- 表示文件名 对应 头节点所在storage+index
    ```
  - 持久化：暂无
    

- 解决并发写的问题：
  - leader会有同时写的风险，解决方法暂定 —— 细粒度锁
  ```以下策略没能落实，用 n 把锁解决问题
  - 乐观锁：
    - 当写的多方之间并没有重叠，最后能合并即可提交
    - 但如果有重叠，选择某个重叠的用户提交失败重新编辑吧
  - 具体实现：
    - 创建一个map，key表示修改的slice的index，value表示具体的file
    - 当初次添加某个key时可以正常操作，后续添加会检查是否containsKey，如果contains，标记失败返回false即可
  ```
  
> 我怎么知道谁和谁是并发的访问？
  哪两个线程间需要考虑并发？ - 是会使用公共变量的线程 -> 只需要对这些线程上加锁
  原本写时复制的构想是：拦截下来一段时间内并发访问的线程，将这些线程中，后面重复的部分返回修改失败，前面的部分返回修改成功
  问题就是：很难显式的得到并发下的所有请求线程，应该是将算在一个并发的时间范围设置成执行方法的时间

> 如果内存中显式的维护n把锁呢？


## 核心问题记录：
  - 将文件分片依次存储不同storage上太分散了，白白增加了节点宕机影响全局的可能性
    应该尽量将文件存储的紧凑一点，首先满足负载均衡（不会将全部文件挤到一起），其次是相邻的尽量存储在一起
    > 假如规定最多每3个slice可以连续存储呢？对于小文件任然是一个slice不会占用太大空间，对于稍大文件减缓了分片过于分散的问题
  - 所以存储策略修改为：
    - tracker有client发来的片数请求，本身监控每个storage可用区域，如果片数少于等于3，存储在剩余空间最多的storage上，如果片数等于7，分配为3+3+1...
      并且配置信息3可以因存储文件属性不同而进行修改，使得myDFS能够适应更多存储环境
    上面的一切架构不会都要改吧
    - 偷懒日记：
        - 存到哪里和storage无关，所以只要tracker返回给client的目的服务器中表现出需要连续存储，client给storage的指令是基于片的，也可以架构出连续片 
        - 连续？为什么连续？只要在一个storage内不就行了？这样锁的粒度也不需要加到一个线程，而是一个slice
    - 并发写：
        - 上传只需要解决并发请求依次排开，超过storage界限的取消存储即可
        - 怎么能保证请求依次排开并且不会导致阻塞？ -- 每个线程都根据更新后的startIndex直接写入
        
      
