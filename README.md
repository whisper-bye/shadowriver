shadowriver是一个纯JVM/Scala实现的Andorid VPN应用程序。

#### 注意
这是一个试验性质的程序，仅用于学习交流。

#### 缘起

众所周知，安卓系统上很多App都自带了流氓属性，所以想做一个App可以实时监控这些App的流量，搜了一圈，发现安卓的[VPNService](https://developer.android.com/reference/android/net/VpnService.html)可以满足需求，而且无需越狱。

比如著名的[shadowsocks](https://github.com/shadowsocks/shadowsocks-android)也是基于这个原理。

#### 为什么不在SS的基础上改进

SS的Android客户端虽然也是用Scala写的，但只是用于实现了UI和VPNService接口，核心部分是ss-local，ss-tunnel，tun2socks这些C语言工具。

    shell@bullhead:/ $ ps |grep shadowsocks -i

    u0a84    6093  494   938400 18848 SyS_epoll 0000000000 S com.github.shadowsocks:vpn
    u0a84    6158  6093  3636   780   SyS_epoll 0000000000 S /data/user/0/com.github.shadowsocks/ss-local
    u0_a84    6161  6093  8700   456   do_sigtime 0000000000 S /data/user/0/com.github.shadowsocks/pdnsd
    u0a84    6166  6093  3116   612   SyS_epoll 0000000000 S /data/user/0/com.github.shadowsocks/ss-tunnel
    u0a84    6172  6093  3816   880   SyS_epoll 0000000000 S /data/user/0/com.github.shadowsocks/tun2socks

也因此无法方便的在App应用层面获取网络数据包，实现诸如：**实时展示数据包内容**，**修改数据包内容**，**MITM** 这样的功能。

#### 代码结构：

[ShadowRiverVpnService](https://github.com/wuhx/shadowriver/blob/master/src/main/scala/ShadowRiverVpnService.scala)：实现VPNService，创建VPN虚拟网卡接口

[VpnActor](https://github.com/wuhx/shadowriver/blob/master/src/main/scala/actor/VpnActor.scala)：将从VPN接口收到的来自App的数据包（TCP或UDP）发送给TunActor（对应ss-local）

[TunActor](https://github.com/wuhx/shadowriver/blob/master/src/main/scala/actor/TunActor.scala)：负责建立本地到VPN服务器的链接（对应ss-tunnel）

[tcpip](https://github.com/wuhx/shadowriver/tree/master/src/main/scala/tcpip)：实现了一个基于Scala的Tcp/IP协议栈（对应tun2socks）。


#### 代码完成度：

最后一次测试：成功通过VPN打开几个网页后，会挂死，logcat显示Android一直在GC。测试机为一台Nexus5，换一台内存大一点的手机，效果可能会好一点。但离可用还很远。


#### 遗留问题的一点思路：

反复GC的原因是ShadowRiver占用了太多内存，这也很好理解，ShadowRiver把整个TCP/IP都搬到JVM内，App每收发一个数据包都需要在JVM上占用一块内存，频繁的收发数据包很容易产生大量的碎片内存，给JVM的垃圾回收造成压力。

意识到这个问题后，我创建了一个[内存池](https://github.com/wuhx/shadowriver/blob/master/src/main/scala/actor/DirectByteBufferPool.scala)，并且使用Direct Buffer直接分配堆外内存，VPN每次收发数据包时不直接分配内存，而是从内存池获取一个预分配的内存，用完后再丢回去，留给后续数据包继续用。这样做有一些效果（可以打开几个页面了）但还没解决问题。

#### 编译运行

`sbt run`

#### 有任何问题和建议请提ISSUE，感谢!

相关链接

[自己动手写VPN](http://xun.im/2016/06/02/write-your-own-vpn/)

[服务端](https://github.com/wuhx/vpnfromscratch-server)