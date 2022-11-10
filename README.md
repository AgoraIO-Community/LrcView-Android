# LrcView-Android

parsing and displaying lyrics dynamically

# Deprecated

请参考 [https://github.com/AgoraIO-Community/LyricsView](https://github.com/AgoraIO-Community/LyricsView) 获取最新版本，本仓库不再继续更新

# 简介

Agora 歌词控件（lrcview）支持在歌曲播放的同时同步显示 LRC 或 XML 格式的歌词。本文介绍如何在项目中集成并使用 Agora 歌词控件。

功能描述

歌曲播放时，根据当前播放进度显示对应的歌词
手势拖动到指定时间的歌词，歌曲进度随之改变
自定义歌词界面布局
自定义更换歌词背景
实现方法

## 引入 lrcview 控件

### 源代码模式

参考如下步骤，在主播端和观众端添加 lrcview 控件：

将 Online KTV 下的 lrcview 文件夹拷贝至你的项目文件夹下。
在你的项目中引入 lrcview 控件。
打开项目的 settings.gradle 文件，添加如下代码：

include ':lrcview'
在你的项目中添加 lrcview 控件的依赖项。打开项目的 app/build.gradle 文件，添加如下代码：
dependencies {
......
implementation project(':lrcview')
}

### JitPack 方式

TBD

## 自定义 lrcview 控件界面布局

在项目的 Activity 中，自定义 lrcview 控件的界面布局。示例代码如下：

```
<io.agora.lrcview.LrcView
android:id="@+id/lrcView"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:paddingStart="10dp"
android:paddingTop="20dp"
android:paddingEnd="10dp"
android:paddingBottom="20dp"
//当前行歌词颜色
app:lrcCurrentTextColor="@color/ktv_lrc_highligh"
//歌词行间距
app:lrcDividerHeight="20dp"
//无歌词情况下的默认文字
app:lrcLabel="暂无歌词"
//非当前行歌词颜色
app:lrcNormalTextColor="@color/ktv_lrc_nomal"
//非当前行歌词字体大小
app:lrcNormalTextSize="16sp"
//歌词对齐方式
app:lrcTextGravity="center"
//当前行歌词字体大小
app:lrcTextSize="26sp" />
声明和初始化 lrcview 控件对象
```

在项目的 Activity 中，声明和初始化 lrcview 控件对象。示例代码如下：

```
public class LiveActivity extends RtcBaseActivity {
private LrcView mLrcView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ...
        mLrcView = findViewById(R.id.lrc_view);
        ...
    }
}
```

## 打分回调

```
public interface OnActionListener {
/** 咪咕歌词原始参考pitch值回调, 用于开发者自行实现打分逻辑. 歌词每个tone回调一次
* pitch: 当前tone的pitch值
* totalCount: 整个xml的tone个数, 用于开发者方便自己在app层计算平均分.
*/
void onOriginalPitch(double pitch, int totalCount);


/** paas组件内置的打分回调, 每句歌词结束的时候提供回调(句指xml中的sentence节点), 并提供totalScore参考值用于按照百分比方式显示分数
* score: 这次回调的分数 40-100之间
* cumulativeScore: 累计的分数 初始分累计到当前的分数
* total: 总分 = 初始分(默认值0分) + xml中sentence的个数 * 100
* 当开启分数回调后, 可拖动功能失效
*/
void onScore(double score, double cumulativeScore, double totalScore);

}


初始分默认为0分, 如果要重定义在下面:
<io.agora.lrcview.LrcView
        android:id="@+id/lrcView"
        app:lrcDefaultScore="0"
        app:lrcEnableDrag="true"
        app:lrcScore="true"
 />
```

核心 API 参考如下：

| API                                 | 实现功能                                                                                                                                                                 |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| setActionListener                   | 订阅 OnActionListener 回调事件。OnActionListener 为 lrcview 控件回调类，包括：歌词加载完成回调、进度改变回调、开始拖动歌词回调、结束拖动歌词回调。                       |
| setEnableDrag                       | 设置是否允许上下拖动歌词。                                                                                                                                               |
| setTotalDuration                    | 设置歌词总时长，单位毫秒。必须与歌曲时长一致。                                                                                                                           |
| setNormalColor                      | 设置非当前行歌词字体颜色。                                                                                                                                               |
| setNormalTextSize                   | 普通歌词文本字体大小。                                                                                                                                                   |
| setCurrentTextSize                  | 当前歌词文本字体大小。                                                                                                                                                   |
| setCurrentColor                     | 设置当前行歌词的字体颜色。                                                                                                                                               |
| setLabel                            | 设置歌词为空时屏幕中央显示的文字，如“暂无歌词”。                                                                                                                         |
| setLrcData                          | 手动设置歌词数据。                                                                                                                                                       |
| reset                               | 重置内部状态，清空已经加载的歌词。                                                                                                                                       |
| loadLrc(mainLrcText, secondLrcText) | 加载本地歌词文件。 支持加载 LRC 格式的双语歌词，mainLrcText 是中文歌词对象，secondLrcText 是英文歌词对象。对于非双语歌词， 将 mainLrcText 或 secondLrcText 设置为 null。 |
| onLoadLrcCompleted                  | 歌词文件加载完成回调。                                                                                                                                                   |
| updateTime                          | 根据当前歌曲播放进度更新歌词进度，单位为毫秒。                                                                                                                           |
| hasLrc                              | 获取歌词文件状态。 true：歌词有效 false：歌词无效，无法播放 reset 重置内部状态，清空已经加载的歌词。                                                                     |
