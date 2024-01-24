# Pepper 机器人开发踩坑指北
> MS Garage Intern [XiangYu](mailto:albertmilagro@outlook.com)


## 一、机器人密钥
在开发过程中可能会需要例如SSH密钥，机器人密码，SBR账号等，相关   `User` 和 `Password`信息已经使用便利贴在了机器人平板背后。

## 二、机器人概述
MS Garage 的人形机器人是由Soft Bank 公司开发的产品Pepper。包括头部和平板两套系统。在开发中，暴露给开发者的是平板的`NaoQi2.9`系统，即开发者只需要进行`Android`开发即可，通过在安卓程序的代码中调用机器人的肢体，语言等接口。~~平板会自动给机器人头部发送控制指令（这部分不需要开发者关注）。~~

由于具有头部和胸前平板两套设备，建议连接到同一个Wi-Fi环境下（头部联网方式在 `设置-机器人`界面），联网后，可以通过系统状态栏查看IP信息：
| IP名          | 对应设备 | 用处        |
| ------------- | -------- | ----------- |
| For Run/Debug | 平板     | adb软件开发 |
| For Robot     | 头部     | SSH登录     |



## 三、开发环境配置 **（有坑）**

1. **请保证你的电脑与机器人在同一局域网中**
2. 请参考[官方文档](https://docs.softbankrobotics.com.cn/pepper/2.9/quickstart/android_sdk.html)完成开发环境的配置，略。
3. 在经历艰辛的环境配置后，你一定迫不及待想要[创建第一个机器人程序](https://docs.softbankrobotics.com.cn/pepper/2.9/quickstart/pepper_qisdk_dev.html)，但是在这一步我似乎踩坑：
   在按照教程[连接机器人](https://docs.softbankrobotics.com.cn/pepper/2.9/quickstart/running_application.html)时，遇到问题，输入机器人密码后，**打开虚拟窗口时直接程序闪退**。无奈本人水平不足，只能不再使用此界面，索性直接[在控制台adb连接]()到平板IP地址，然后在AndroidStudio内已经可以找到连接的设备`ARTNCORE_LPT_200AR`。
4. 编译第一个应用程序时再次遇到gradle报错，主要问题是**无法导入QiSDK**, 经过一番彻夜Google，终于找了[解决方案](https://stackoverflow.com/questions/69377007/getting-error-in-build-of-android-studio-app-for-pepper)：请在`settings.gradle`文件中的`repositories{}`添加以下来源：
   ```gradle
   maven {
        url 'http://android.aldebaran.com/sdk/maven'
        allowInsecureProtocol = true
    }
   ```
5. 如果遇到新的报错，你可能需要在`gradle.properties`中添加一行配置：
   ```properties
   android.enableJetifier=true
   ```
6. 至此，程序应该可以愉快地运行了，如果你仍然遇到问题，可以试着采用和笔者一样的环境和版本（如下图）：
   <figure class="half">
    <img src= "https://milagro-pics.oss-cn-beijing.aliyuncs.com/img/2023-04-23-22-03-14_32afdd5d.png" height="300">
    <img src= "https://milagro-pics.oss-cn-beijing.aliyuncs.com/img/2023-04-23-22-03-33_ade16b9a.png" height="300">
    </figure>



## 4. ChatGPT对话代码

> 请忽略我稀烂的代码结构以及凌乱的执行逻辑。笔者是Java新手以及编程菜鸡，能用就行，不需要发邮件来骂我QAQ
1. 代码文件结构（这里只列出需要关注的核心文件）
   ```bash
    | AndroidManifest.xml   // 一些权限声明
    ├─java
    │  └─com
    │      └─example
    │          └─speech_rec
    │                  AzureGPT.java    // Azure的OpenAI API封装
    │                  AzureSpeechToText.java   // 听觉，语音转文字
    │                  AzureTextToSpeech.java   // 说话，Azure语音合成
    │                  *ChatGPT.java // OpenAI API封装[deprecated]
    │                  MainActivity.java    // 主函数
    │
    ├─assets
    |      config.properties  // 配置文件，在这里填写你的Key
    |
    └─res
        ├─layout
        │      activity_main.xml    // 用户界面UI
        │
        ├─raw
        │      affirmation_a001.qianim  // 文件夹存一些.qianim动作资源
        │
        ├─values
        │      colors.xml   // 一些UI用到的色彩规定
        │      strings.xml  //  一些UI的文字（平板英文模式）
        ├─values-zh
        │      strings.xml  //  一些UI的文字（平板中文模式）
   ```
2. 运行代码需要的配置项
   
   需要在`/assets/config.properties`填写你自己的Azure OpenAI token，Azure认知服务token等信息
   



## 五、可能需要的帮助链接

> ~~文档都看到这里了，预祝工作愉快~~
1. [Pepper中文开发文档](https://docs.softbankrobotics.com.cn/)
2. [QiSDK开发文档](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/index.html)
3. [Pepper开发视频教程](https://www.bilibili.com/video/BV12k4y1z77T/?spm_id_from=333.788&vd_source=13659ee5a237e839c02cd066ef47658e)
4. [Android开发者指南](https://developer.android.google.cn/guide?hl=zh-cn) ~~似乎只支持Kotlin了，但是依然可以学一下Android开发~~