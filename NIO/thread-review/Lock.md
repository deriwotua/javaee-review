[TOC]



## 前言



通过特性将锁进行分组归类：

![根据锁特性进行分组归类](./assets/根据锁特性进行分组归类.png)

## 乐观锁 VS 悲观锁

乐观锁与悲观锁是一种广义上的概念，体现了看待线程同步的不同角度。在Java和数据库中都有此概念对应的实际应用。

先说概念。对于同一个数据的并发操作，**悲观锁认为自己在使用数据的时候一定有别的线程来修改数据**，因此在获取数据的时候会先加锁，确保数据不会被别的线程修改。Java中，`synchronized`关键字和`Lock`的实现类都是悲观锁。

而**乐观锁认为自己在使用数据时不会有别的线程修改数据，所以不会添加锁，只是在更新数据的时候去判断之前有没有别的线程更新了这个数据**。如果这个数据没有被更新，当前线程将自己修改的数据成功写入。如果数据已经被其他线程更新，则根据不同的实现方式执行不同的操作（例如报错或者自动重试）。

乐观锁在Java中是通过使用无锁编程来实现，最常采用的是CAS算法，Java原子类中的递增操作就通过CAS自旋实现的。

![乐观锁VS悲观锁](./assets/乐观锁VS悲观锁.png)

从概念描述可发现
- 悲观锁适合写操作多的场景，先加锁可以保证写操作时数据正确。
- 乐观锁适合读操作多的场景，不加锁的特点能够使其读操作的性能大幅提升。

```java
// ------------------------- 悲观锁的调用方式 -------------------------
// synchronized
public synchronized void testMethod() {
	// 操作同步资源
}
// ReentrantLock
private ReentrantLock lock = new ReentrantLock(); // 需要保证多个线程使用的是同一个锁
public void modifyPublicResources() {
	lock.lock();
	// 操作同步资源
	lock.unlock();
}

// ------------------------- 乐观锁的调用方式 -------------------------
private AtomicInteger atomicInteger = new AtomicInteger();  // 需要保证多个线程使用的是同一个AtomicInteger
atomicInteger.incrementAndGet(); //执行自增1
```

- 悲观锁基本都是在显式的锁定之后再操作同步资源，而乐观锁则直接去操作同步资源。那么，为何乐观锁能够做到不锁定同步资源也可以正确的实现线程同步呢？乐观锁的主要实现方式 `CAS` 的技术原理

`CAS`全称 `Compare And Swap`（比较与交换），是一种无锁算法。在不使用锁（没有线程被阻塞）的情况下实现多线程之间的变量同步。`java.util.concurrent`包中的原子类就是通过`CAS`来实现了乐观锁。

`CAS`算法涉及到三个操作数：

- 需要读写的内存值 `V`。
- 进行比较的值 `A`。
- 要写入的新值 `B`。

当且仅当 `V` 的值等于 `A` 时，`CAS`通过原子方式用新值`B`来更新`V`的值（“比较+更新”整体是一个原子操作），否则不会执行任何操作。一般情况下，“更新”是一个不断重试的操作。

`java.util.concurrent`包中的原子类，就是通过`CAS`来实现了乐观锁，原子类`AtomicInteger`的源码，看一下`AtomicInteger`的定义：

```java
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

	// 通过Unsafe获取并操作内存的数据
    // setup to use Unsafe.compareAndSwapInt for updates
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 存储value在AtomicInteger中的偏移量
    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

	// 存储AtomicInteger的int值，该属性需要借助volatile关键字保证其在线程间是可见的
    private volatile int value;
```

查看`AtomicInteger`的自增函数`incrementAndGet()`的源码时，发现自增函数底层调用的是`unsafe.getAndAddInt()`。但是由于JDK本身只有`Unsafe.class`，只通过`.class`文件中的参数名，并不能很好的了解方法的作用，所以通过OpenJDK 8 来查看`Unsafe`的源码

```java
// ------------------------- JDK 8 -------------------------
// java.util.concurrent.atomic.AtomicInteger#incrementAndGet 自增方法
public final int incrementAndGet() {
  return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
}

// Unsafe.class
public final int getAndAddInt(Object var1, long var2, int var4) {
  int var5;
  do {
      var5 = this.getIntVolatile(var1, var2);
  } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));
  return var5;
}

// ------------------------- OpenJDK 8 -------------------------
// Unsafe.java
public final int getAndAddInt(Object o, long offset, int delta) {
   int v;
   do {
       v = getIntVolatile(o, offset); // 获取给定对象o中的偏移量处的值v
   } while (!compareAndSwapInt(o, offset, v, v + delta));
   return v;
}
```

根据OpenJDK 8的源码可以看出，`getAndAddInt()`循环获取给定对象`o`中的偏移量处的值`v`，然后判断内存值是否等于`v`。如果相等则将内存值设置为` v + delta`，否则返回`false`，继续循环进行重试，直到设置成功才能退出循环，并且将旧值返回。整个“比较+更新”操作封装在`compareAndSwapInt()`中，在JNI里是借助于一个CPU指令完成的，属于原子操作，可以保证多个线程都能够看到同一个变量的修改值。

后续JDK通过CPU的`cmpxchg`指令，去比较寄存器中的 `A` 和 内存中的值 `V`。如果相等，就把要写入的新值 `B` 存入内存中。如果不相等，就将内存值 `V` 赋值给寄存器中的值 `A`。然后通过Java代码中的`while`循环再次调用`cmpxchg`指令进行重试，直到设置成功为止。

```cpp
// JDK8 unsafe.cpp  cmpxchg = compare and exchange
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x))
  UnsafeWrapper("Unsafe_CompareAndSwapInt");
  oop p = JNIHandles::resolve(obj);
  jint* addr = (jint *) index_oop_from_field_offset_long(p, offset);
  return (jint)(Atomic::cmpxchg(x, addr, e)) == e;
UNSAFE_END

// JDK8 atomic_linux_x86.inline.hpp **93行
inline jint Atomic::cmpxchg (jint exchange_value, volatile jint* dest, jint compare_value) {
  // is_MP = Multi Processor  
  int mp = os::is_MP();
  __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp)
                    : "cc", "memory");
  return exchange_value;
}

// JDK8 os.hpp is_MP()
static inline bool is_MP() {
    // During bootstrap if _processor_count is not yet initialized
    // we claim to be MP as that is safest. If any platform has a
    // stub generator that might be triggered in this phase and for
    // which being declared MP when in fact not, is a problem - then
    // the bootstrap routine for the stub generator needs to check
    // the processor count directly and leave the bootstrap routine
    // in place until called after initialization has ocurred.
    return (_processor_count != 1) || AssumeMP;
}

// JDK8 atomic_linux_x86.inline.hpp
#define LOCK_IF_MP(mp) "cmp $0, " #mp "; je 1f; lock; 1: "

// 最终实现 cmpxchg = cas 修改变量值
lock cmpxchg 指令
```





`CAS`虽然很高效，但是它也存在三大问题

- `ABA`问题。`CAS`需要在操作值的时候检查内存值是否发生变化，没有发生变化才会更新内存值。但是如果内存值原来是`A`，后来变成了`B`，然后又变成了`A`，那么`CAS`进行检查时会发现值没有发生变化，但是实际上是有变化的。`ABA`问题的解决思路就是在变量前面添加版本号，每次变量更新的时候都把版本号加一，这样变化过程就从`A－B－A`变成了`1A－2B－3A`。
  - JDK从1.5开始提供了`AtomicStampedReference`类来解决`ABA`问题，具体操作封装在`compareAndSet()`中。`compareAndSet()`首先检查当前引用和当前标志与预期引用和预期标志是否相等，如果都相等，则以原子方式将引用值和标志的值设置为给定的更新值。
- 循环时间长开销大。`CAS`操作如果长时间不成功，会导致其一直自旋，给CPU带来非常大的开销。
- 只能保证一个共享变量的原子操作。对一个共享变量执行操作时，`CAS`能够保证原子操作，但是对多个共享变量操作时，`CAS`是无法保证操作的原子性的。
  - Java从1.5开始JDK提供了`AtomicReference`类来保证引用对象之间的原子性，可以把多个变量放在一个对象里来进行`CAS`操作。


## 自旋锁 VS 适应性自旋锁

**阻塞或唤醒一个Java线程需要操作系统切换CPU状态来完成**，这种状态转换需要耗费处理器时间。如果同步代码块中的内容过于简单，状态转换消耗的时间有可能比用户代码执行的时间还要长。

在许多场景中，同步资源的锁定时间很短，为了这一小段时间去切换线程，线程挂起和恢复现场的开销可能会让系统得不偿失。**如果物理机器有多个处理器，能够让两个或以上的线程同时并行执行，就可以让后面那个请求锁的线程不放弃CPU的执行时间，看看持有锁的线程是否很快就会释放锁**。

而为了让当前线程“稍等一下”，需让当前线程进行自旋，如果在自旋完成后前面锁定同步资源的线程已经释放了锁，那么当前线程就可以不必阻塞而是直接获取同步资源，从而避免切换线程的开销，这就是自旋锁。

![自旋锁VS适应性自旋锁](./assets/自旋锁VS适应性自旋锁.png)

自旋锁本身是有缺点的，它不能代替阻塞。自旋等待虽然避免了线程切换的开销，但它要**占用处理器时间**。如果锁被占用的时间很短，自旋等待的效果就会非常好。反之，**如果锁被占用的时间很长，那么自旋的线程只会白浪费处理器资源**。所以，自旋等待的时间必须要有一定的限度，如果自旋超过了限定次数（默认是10次，可以使用`-XX:PreBlockSpin`来更改）没有成功获得锁，就应当挂起线程。

自旋锁的实现原理同样也是`CAS`，**`AtomicInteger`中调用`Unsafe#getAndAddInt()`进行自增操作的源码中的`do-while`循环就是一个自旋操作**，如果修改数值失败则通过循环来执行自旋，直至修改成功。

```java
// ------------------------- OpenJDK 8 -------------------------
// Unsafe.java
public final int getAndAddInt(Object o, long offset, int delta) {
   int v;
   do { // do-while循环就是一个自旋操作
       v = getIntVolatile(o, offset); // 获取给定对象o中的偏移量处的值v
   } while (!compareAndSwapInt(o, offset, v, v + delta));
   return v;
}
```

自旋锁在JDK1.4.2中引入，使用`-XX:+UseSpinning`来开启。JDK 6中变为默认开启，并且引入了自适应的自旋锁（适应性自旋锁）。

自适应意味着自旋的时间（次数）不再固定，而是**由前一次在同一个锁上的自旋时间及锁的拥有者的状态来决定**。如果在同一个锁对象上，自旋等待刚刚成功获得过锁，并且持有锁的线程正在运行中，那么虚拟机就会认为这次自旋也是很有可能再次成功，进而它将允许自旋等待持续相对更长的时间。如果对于某个锁，自旋很少成功获得过，那在以后尝试获取这个锁时将可能省略掉自旋过程，直接阻塞线程，避免浪费处理器资源。

在自旋锁中另有三种常见的锁形式: `TicketLock`、`CLHlock`和`MCSlock`，这里仅做名词介绍，不做深入讲解，感兴趣可自行查阅相关资料。

## 无锁 VS 偏向锁 VS 轻量级锁 VS 重量级锁
> 这四种锁是指锁的状态，专门针对`synchronized`

首先为什么`Synchronized`能实现线程同步？在回答这个问题之前需要了解两个重要的概念：`Java对象头`、`Monitor`。

Java对象头
- `synchronized`是悲观锁，在操作同步资源之前需要给同步资源先加锁，这把锁就是存在`Java对象头`里的，而Java对象头又是什么呢？
- 以Hotspot虚拟机为例，Hotspot的对象头主要包括两部分数据：`Mark Word`标记字段、`Klass Pointer`类型指针。
  - `Mark Word`：默认存储对象的`HashCode`，`分代年龄`和`锁标志位信息`。这些信息都是与对象自身定义无关的数据，所以`Mark Word`被设计成一个**非固定的数据结构**以便在极小的空间内存存储尽量多的数据。它会根据对象的状态复用自己的存储空间，也就是说在运行期间`Mark Word`里存储的数据会随着锁标志位的变化而变化。

  - `Klass Point`：对象指向它的类元数据的指针，虚拟机通过这个指针来确定这个对象是哪个类的实例。

Monitor
- `Monitor`可以理解为一个`同步工具`或一种`同步机制`，通常被描述为一个对象。每一个Java对象就有一把看不见的锁，称为内部锁或者Monitor锁。

- `Monitor`是线程私有的数据结构，每一个线程都有一个可用`monitor record`列表，同时还有一个全局的可用列表。每一个被锁住的对象都会和一个`monitor`关联，同时`monitor`中有一个`Owner`字段存放拥有该锁的线程的唯一标识，表示该锁被这个线程占用。

现在话题回到`synchronized`，`synchronized`通过`Monitor`来实现线程同步，`Monitor`是依赖于底层的操作系统的`Mutex Lock`互斥锁来实现的线程同步。

如同在自旋锁中提到的**阻塞或唤醒一个Java线程需要操作系统切换CPU状态来完成，这种状态转换需要耗费处理器时间。如果同步代码块中的内容过于简单，状态转换消耗的时间有可能比用户代码执行的时间还要长**。这种方式就是`synchronized`最初实现同步的方式，这就是JDK 6之前`synchronized`效率低的原因。这种依赖于操作系统`Mutex Lock`所实现的锁称之为`重量级锁`，JDK 6中为了减少获得锁和释放锁带来的性能消耗，引入了`偏向锁`和`轻量级锁`。

所以目前锁一共有4种状态，级别从低到高依次是：`无锁`、`偏向锁`、`轻量级锁`和`重量级锁`。锁状态只能升级不能降级。

`synchronized`的加锁机制以及相关知识有了一个了解，那么下面给出四种锁状态对应的`Mark Word`内容

锁状态	| Mark Word存储内容	|存储内容
--------|---------------------------------|----
无锁	| 对象的hashCode、对象分代年龄、是否是偏向锁（0）	| 01
偏向锁	| 偏向线程ID、偏向时间戳、对象分代年龄、是否是偏向锁（1）	| 01
轻量级锁	| 指向栈中锁记录的指针	| 00
重量级锁	| 指向互斥量（重量级锁）的指针	| 10



**无锁**
- 无锁没有对资源进行锁定，所有的线程都能访问并修改同一个资源，但同时只有一个线程能修改成功。

- 无锁的特点就是修改操作在循环内进行，线程会不断的尝试修改共享资源。如果没有冲突就修改成功并退出，否则就会继续循环尝试。如果有多个线程修改同一个值，必定会有一个线程能修改成功，而其他修改失败的线程会不断重试直到修改成功。上面介绍的`CAS`原理及应用即是无锁的实现。无锁无法全面代替有锁，但无锁在某些场合下的性能是非常高的。

**偏向锁**
- 偏向锁是指一段同步代码一直被一个线程所访问，那么该线程会自动获取锁，降低获取锁的代价。

- 在大多数情况下，锁总是由同一线程多次获得，不存在多线程竞争，所以出现了偏向锁。其目标就是**在只有一个线程执行同步代码块时能够提高性能**。

- 当一个线程访问同步代码块并获取锁时，会在`Mark Word`里存储**锁偏向的线程ID**。**在线程进入和退出同步块时不再通过`CAS`操作来加锁和解锁，而是检测`Mark Word`里是否存储着指向当前线程的偏向锁**。引入偏向锁是为了在无多线程竞争的情况下尽量减少不必要的轻量级锁执行路径，因为轻量级锁的获取及释放依赖多次`CAS`原子指令，而偏向锁只需要在置换`ThreadID`的时候依赖一次`CAS`原子指令即可。

- 偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁，线程不会主动释放偏向锁。偏向锁的撤销，需要等待全局安全点（在这个时间点上没有字节码正在执行），它会首先暂停拥有偏向锁的线程，判断锁对象是否处于被锁定状态。撤销偏向锁后恢复到无锁（标志位为`01`）或轻量级锁（标志位为`00`）的状态。

- 偏向锁在JDK 6及以后的JVM里是默认启用的。可以通过JVM参数关闭偏向锁`-XX:-UseBiasedLocking=false`，关闭之后程序默认会进入轻量级锁状态。


**轻量级锁**
- 是指当锁是偏向锁的时候，被另外的线程所访问，偏向锁就会升级为轻量级锁，其他线程会通过自旋的形式尝试获取锁，不会阻塞，从而提高性能。

- 在代码进入同步块的时候，如果同步对象锁状态为无锁状态（锁标志位为`01`状态，是否为偏向锁为`0`），虚拟机首先将在当前线程的栈帧中建立一个名为`Lock Record`锁记录的空间，用于存储锁对象目前的`Mark Word`的拷贝，然后拷贝对象头中的`Mark Word`复制到锁记录中。

- 拷贝成功后，虚拟机将使用`CAS`操作尝试将对象的`Mark Word`更新为指向`Lock Record`的指针，并将`Lock Record`里的`owner`指针指向对象的`Mark Word`。

- 如果这个更新动作成功了，那么这个线程就拥有了该对象的锁，并且对象`Mark Word`的锁标志位设置为`00`，表示此对象处于轻量级锁定状态。

- 如果轻量级锁的更新操作失败了，虚拟机首先会检查对象的`Mark Word`是否指向当前线程的栈帧，如果是就说明当前线程已经拥有了这个对象的锁，那就可以直接进入同步块继续执行，否则说明多个线程竞争锁。

- 若当前只有一个等待线程，则该线程通过自旋进行等待。但是当自旋超过一定的次数，或者一个线程在持有锁，一个在自旋，又有第三个来访时，轻量级锁升级为重量级锁。

**重量级锁**

- 升级为重量级锁时，锁标志的状态值变为`10`，此时`Mark Word`中存储的是指向重量级锁的指针，此时等待锁的线程都会进入阻塞状态。

整体的锁状态升级流程
```
无锁 -----> 偏向锁 -----> 轻量级锁 -----> 重量级锁
```

**偏向锁通过对比`Mark Word`解决加锁问题，避免执行`CAS`操作。而轻量级锁是通过用`CAS`操作和自旋来解决加锁问题，避免线程阻塞和唤醒而影响性能。重量级锁是将除了拥有锁的线程以外的线程都阻塞。**

## 公平锁 VS 非公平锁

公平锁是指多个线程按照申请锁的顺序来获取锁，线程直接进入队列中排队，队列中的第一个线程才能获得锁。公平锁的优点是等待锁的线程不会饿死。缺点是整体吞吐效率相对非公平锁要低，等待队列中除第一个线程以外的所有线程都会阻塞，CPU唤醒阻塞线程的开销比非公平锁大。

非公平锁是多个线程加锁时直接尝试获取锁，获取不到才会到等待队列的队尾等待。但如果此时锁刚好可用，那么这个线程可以无需阻塞直接获取到锁，所以非公平锁有可能出现后申请锁的线程先获取锁的场景。非公平锁的优点是可以减少唤起线程的开销，整体的吞吐效率高，因为**线程有几率不阻塞直接获得锁**，CPU不必唤醒所有线程。缺点是处于等待队列中的线程可能会饿死，或者等很久才会获得锁。

- 假设有一口水井，有管理员看守，管理员有一把锁，只有拿到锁的人才能够打水，打完水要把锁还给管理员。每个过来打水的人都要管理员的允许并拿到锁之后才能去打水，如果前面有人正在打水，那么这个想要打水的人就必须排队。管理员会查看下一个要去打水的人是不是队伍里排最前面的人，如果是的话，才会给你锁让你去打水；如果你不是排第一的人，就必须去队尾排队，这就是公平锁

  ![公平锁](./assets/公平锁.png)

- 对于非公平锁，管理员对打水的人没有要求。即使等待队伍里有排队等待的人，但如果在上一个人刚打完水把锁还给管理员而且管理员还没有允许等待队伍里下一个人去打水时，刚好来了一个插队的人，这个插队的人是可以直接从管理员那里拿到锁去打水，不需要排队，原本排队等待的人只能继续等待。

  ![非公平锁](./assets/非公平锁.png)


`ReentrantLock`源码中的公平锁和非公平锁
```java
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    /**...*/
    abstract static class Sync extends AbstractQueuedSynchronizer {...}

    /** 非公平锁 */
    static final class NonfairSync extends Sync {...}

    /** 公平锁 */
    static final class FairSync extends Sync {...}

    /**...*/
    public ReentrantLock() {
        // 默认非公平锁
        sync = new NonfairSync();
    }

    /**...*/
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
```

`ReentrantLock`里面有一个内部类`Sync`，`Sync`继承`AQS`（AbstractQueuedSynchronizer），添加锁和释放锁的大部分操作实际上都是在`Sync`中实现的。它有公平锁`FairSync`和非公平锁`NonfairSync`两个子类。`ReentrantLock`默认使用非公平锁，也可以通过构造器来显示的指定使用公平锁。

公平锁与非公平锁的加锁方法的源码

![公平锁与非公平锁的加锁方法源码](./assets/公平锁与非公平锁的加锁方法源码.png)

- 可以明显的看出公平锁与非公平锁的lock()方法唯一的区别就在于公平锁在获取同步状态时多了一个限制条件：`hasQueuedPredecessors()`
  ```java
  /**
   * 判断当前线程是否位于同步队列中的第一个。
   * 如果是则返回true，否则返回false
   */
  public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized
    // before tail and on head.next being accurate if the current
    // thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
  }
  ```

**公平锁就是通过同步队列来实现多个线程按照申请锁的顺序来获取锁，从而实现公平的特性。非公平锁加锁时不考虑排队等待问题，直接尝试获取锁，所以存在后申请却先获得锁的情况。**



## 可重入锁 VS 非可重入锁

可重入锁又名递归锁，是指在同一个线程在外层方法获取锁的时候，再进入该线程的内层方法会自动获取锁（前提锁对象得是同一个对象或者class），不会因为之前已经获取过还没释放而阻塞。Java中`ReentrantLock`和`synchronized`都是可重入锁，可重入锁的一个优点是可一定程度避免死锁。
```java
public class Widget {
    public synchronized void doSomething() {
        System.out.println("方法1执行...");
        doOthers();
    }

    public synchronized void doOthers() {
        System.out.println("方法2执行...");
    }
}
```
- 类中的两个方法都是被内置锁`synchronized`修饰的，`doSomething()`方法中调用`doOthers()`方法。因为内置锁是可重入的，所以同一个线程在调用`doOthers()`时可以直接获得当前对象的锁，进入`doOthers()`进行操作。

- 如果是一个不可重入锁，那么当前线程在调用`doOthers()`之前需要将执行`doSomething()`时获取当前对象的锁释放掉，实际上该对象锁已被当前线程所持有，且无法释放。所以此时会出现死锁。

而为什么可重入锁就可以在嵌套调用时可以自动获得锁呢？
- 还是打水的例子，有多个人在排队打水，此时管理员允许锁和同一个人的多个水桶绑定。这个人用多个水桶打水时，第一个水桶和锁绑定并打完水之后，第二个水桶也可以直接和锁绑定并开始打水，所有的水桶都打完水之后打水人才会将锁还给管理员。这个人的所有打水流程都能够成功执行，后续等待的人也能够打到水。这就是可重入锁。

  ![可重入锁](./assets/可重入锁.png)

- 但如果是非可重入锁的话，此时管理员只允许锁和同一个人的一个水桶绑定。第一个水桶和锁绑定打完水之后并不会释放锁，导致第二个水桶不能和锁绑定也无法打水。当前线程出现死锁，整个等待队列中的所有线程都无法被唤醒。

  ![非可重入锁](./assets/非可重入锁.png)

`ReentrantLock`和`synchronized`都是重入锁，那么通过重入锁`ReentrantLock`以及非可重入锁`NonReentrantLock`的源码来对比分析一下为什么非可重入锁在重复调用同步资源时会出现死锁。

- 首先`ReentrantLock`和`NonReentrantLock`都继承父类`AQS`，其父类`AQS`中维护了一个同步状态`status`来计数重入次数，`status`初始值为`0`。

- 当线程尝试获取锁时，可重入锁先尝试获取并更新`status`值，如果`status == 0`表示没有其他线程在执行同步代码，则把`status`置为`1`，当前线程开始执行。如果`status != 0`，则判断当前线程是否是获取到这个锁的线程，如果是的话执行`status+1`，且当前线程可以再次获取锁。而非可重入锁是直接去获取并尝试更新当前`status`的值，如果`status != 0`的话会导致其获取锁失败，当前线程阻塞。

- 释放锁时，可重入锁同样先获取当前`status`的值，在当前线程是持有锁的线程的前提下。如果`status-1 == 0`，则表示当前线程所有重复获取锁的操作都已经执行完毕，然后该线程才会真正释放锁。而非可重入锁则是在确定当前线程是持有锁的线程之后，直接将`status`置为`0`，将锁释放。

![可重入锁VS非可重入锁源码流程](./assets/可重入锁VS非可重入锁源码流程.png)


## 独享锁 VS 共享锁

独享锁也叫排他锁，是指该锁一次只能被一个线程所持有。如果线程`T`对数据`A`加上排它锁后，则其他线程不能再对`A`加任何类型的锁。获得排它锁的线程即能读数据又能修改数据。JDK中的`synchronized`和JUC中`Lock`的实现类就是互斥锁。

共享锁是指该锁可被多个线程所持有。如果线程`T`对数据`A`加上共享锁后，则其他线程只能对`A`再加共享锁，不能加排它锁。获得共享锁的线程只能读数据，不能修改数据。

独享锁与共享锁也是通过`AQS`来实现的，通过实现不同的方法，来实现独享或者共享。

`ReentrantReadWriteLock`的部分源码

![ReentrantReadWriteLock读写锁](./assets/ReentrantReadWriteLock读写锁.png)

- 可以看到`ReentrantReadWriteLock`有两把锁：`ReadLock`和`WriteLock`，由词知意，一个读锁一个写锁，合称“读写锁”。再进一步观察可以发现`ReadLock`和`WriteLock`是靠内部类`Sync`实现的锁。`Sync`是`AQS`的一个子类，这种结构在`CountDownLatch`、`ReentrantLock`、`Semaphore`里面也都存在。

- 在`ReentrantReadWriteLock`里面，读锁和写锁的锁主体都是`Sync`，但读锁和写锁的加锁方式不一样。读锁是共享锁，写锁是独享锁。读锁的共享锁可保证并发读非常高效，而`读写`、`写读`、`写写`的过程都是互斥，因为读锁和写锁是分离的。所以`ReentrantReadWriteLock`的并发性相比一般的互斥锁有了很大提升。

那读锁和写锁的具体加锁方式有什么区别呢？在了解源码之前需要回顾一下其他知识。在上面提及`AQS`的时候也提到了`state`字段（int类型，32位），该字段用来描述有多少线程获持有锁。

在独享锁中这个值通常是`0`或者`1`（如果是重入锁的话`state`值就是重入的次数），在共享锁中`state`就是持有锁的数量。但是在`ReentrantReadWriteLock`中有读、写两把锁，所以需要在一个整型变量`state`上分别描述读锁和写锁的数量（或者也可以叫状态）。于是将`state`变量“按位切割”切分成了两个部分，高`16`位表示读锁状态（读锁个数），低`16`位表示写锁状态（写锁个数）。

![高低位分别存放读写状态](./assets/高低位分别存放读写状态.png)



了解基本概念后再看写锁的加锁源码

```java
protected final boolean tryAcquire(int acquires) {
	Thread current = Thread.currentThread();
	int c = getState(); // 取到当前锁的个数
	int w = exclusiveCount(c); // 取写锁的个数w
	if (c != 0) { // 如果已经有线程持有了锁(c!=0)
    // (Note: if c != 0 and w == 0 then shared count != 0)
    	// 如果写线程数（w）为0（换言之存在读锁） 或者持有锁的线程不是当前线程就返回失败
		if (w == 0 || current != getExclusiveOwnerThread()) 
			return false;
		// 如果写入锁的数量大于最大数（65535，2的16次方-1）就抛出一个Error。
		if (w + exclusiveCount(acquires) > MAX_COUNT)    
      		throw new Error("Maximum lock count exceeded");
		// Reentrant acquire
    setState(c + acquires);
    return true;
  }
  // 如果当且写线程数为0，并且当前线程需要阻塞那么就返回失败；或者如果通过CAS增加写线程数失败也返回失败。
  if (writerShouldBlock() || !compareAndSetState(c, c + acquires)) 
		return false;
	// 如果c=0，w=0或者c>0，w>0（重入），则设置当前线程或锁的拥有者
	setExclusiveOwnerThread(current); 
	return true;
}
```



* 这段代码首先取到当前锁的个数`c`，然后再通过`c`来获取写锁的个数`w`。因为写锁是低`16`位，所以取低`16`位的最大值与当前的`c`做与运算（ ` int w = exclusiveCount©;` ），高`16`位和`0`与运算后是`0`，剩下的就是低位运算的值，同时也是持有写锁的线程数目。
* 在取到写锁线程的数目后，首先判断是否已经有线程持有了锁。如果已经有线程持有了锁(`c!=0`)，则查看当前写锁线程的数目，如果写线程数为`0`（即此时存在读锁）或者持有锁的线程不是当前线程就返回失败（涉及到公平锁和非公平锁的实现）。
* 如果写入锁的数量大于最大数（`65535`，2的16次方-1）就抛出一个Error。
* 如果当且写线程数为`0`（那么读线程也应该为`0`，因为上面已经处理`c!=0`的情况），并且当前线程需要阻塞那么就返回失败；如果通过`CAS`增加写线程数失败也返回失败。
* 如果`c=0,w=0`或者`c>0,w>0`（重入），则设置当前线程或锁的拥有者，返回成功！



`tryAcquire()`除了重入条件（当前线程为获取了写锁的线程）之外，增加了一个读锁是否存在的判断。如果存在读锁，则写锁不能被获取，原因在于：必须确保写锁的操作对读锁可见，如果允许读锁在已被获取的情况下对写锁的获取，那么正在运行的其他读线程就无法感知到当前写线程的操作。

因此，只有等待其他读线程都释放了读锁，写锁才能被当前线程获取，而写锁一旦被获取，则其他读写线程的后续访问均被阻塞。写锁的释放与`ReentrantLock`的释放过程基本类似，每次释放均减少写状态，当写状态为`0`时表示写锁已被释放，然后等待的读写线程才能够继续访问读写锁，同时前次写线程的修改对后续的读写线程可见。



读锁的源码

```java
protected final int tryAcquireShared(int unused) {
    Thread current = Thread.currentThread();
    int c = getState();
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        // 如果其他线程已经获取了写锁，则当前线程获取读锁失败，进入等待状态
        return -1;                                   
    int r = sharedCount(c);
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    return fullTryAcquireShared(current);
}
```

- 可以看到在`tryAcquireShared(int 
  unused)`方法中，如果其他线程已经获取了写锁，则当前线程获取读锁失败，进入等待状态。如果当前线程获取了写锁或者写锁未被获取，则当前线程（线程安全，依靠`CAS`保证）增加读状态，成功获取读锁。读锁的每次释放（线程安全的，可能有多个读线程同时释放读锁）均减少读状态，减少的值是`1<<16`。所以读写锁才能实现读读的过程共享，而读写、写读、写写的过程互斥。



再回头看一下互斥锁`ReentrantLock`中公平锁和非公平锁的加锁源码

![ReentrantLock中公平锁和非公平锁加锁都是排他锁](./assets/ReentrantLock中公平锁和非公平锁加锁都是排他锁.png)

- `ReentrantLock`虽然有公平锁和非公平锁两种，但是它们添加的都是独享锁。根据源码所示，当某一个线程调用`lock`方法获取锁时，如果同步资源没有被其他线程锁住，那么当前线程在使用`CAS`更新`state`成功后就会成功抢占该资源。而如果公共资源被占用且不是被当前线程占用，那么就会加锁失败。所以可以确定`ReentrantLock`无论读操作还是写操作，添加的锁都是都是独享锁。


## 分段锁

分段锁其实是一种锁的设计，并不是具体的一种锁，对于`ConcurrentHashMap`而言，其并发的实现就是通过分段锁的形式来实现高效的并发操作。以`ConcurrentHashMap`来说一下分段锁的含义以及设计思想，`ConcurrentHashMap`中的分段锁称为`Segment`，它即类似于`HashMap`（JDK7与JDK8中HashMap的实现）的结构，即内部拥有一个`Entry`数组，数组中的每个元素又是一个链表；同时又是一个`ReentrantLock`（`Segment`继承了`ReentrantLock`)。

当需要`put`元素的时候，并不是对整个`hashmap`进行加锁，而是先通过`hashcode`来知道他要放在那一个分段中，然后对这个分段进行加锁，所以当多线程`put`的时候，只要不是放在一个分段中，就实现了真正的并行的插入。

但是，在统计`size`的时候，可就是获取`hashmap`全局信息的时候，就需要获取所有的分段锁才能统计。分段锁的设计目的是细化锁的粒度，当操作不需要更新整个数组的时候，就仅仅针对数组中的一项进行加锁操作。





## 拓展



[深入分析Synchronized原理](./深入分析Synchronized原理.md)



[JAVA锁的膨胀过程和优化](./JAVA锁的膨胀过程和优化.md)



[lock的实现原理](./lock的实现原理.md)





> ## 参考资料 

[不可不说的Java“锁”事](https://tech.meituan.com/2018/11/15/java-lock.html)





















