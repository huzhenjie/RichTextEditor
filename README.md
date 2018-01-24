# RichTextEditor

Android 下的富文本编辑器

## Features

> Redo
> Undo
> Bold
> Bullet
> Clear
> Italic
> Quote
> Underline
> StrikeThrough
> Link

User `Glide 4` lib. Support `gif`

## Screenshots

![](http://biuugames.huya.com/rich_text_editor_preview.gif)

# How to

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

**Step 2.** Add the dependency

```
dependencies {
    compile 'com.github.huzhenjie:RichTextEditor:1.0.3'
}
```

## Example

In your layout

```
<ScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_above="@id/tools"
    android:layout_alignParentTop="true"
    android:fillViewport="true">

    <com.scrat.app.richtext.RichEditText
        android:id="@+id/rich_text"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/transparent"
        android:gravity="top|start"
        android:paddingEnd="16dp"
        android:paddingLeft="16dp"
        android:paddingRight="16dp"
        android:paddingStart="16dp"
        android:paddingTop="16dp"
        android:scrollbars="vertical"
        app:bulletColor="#FF2196F3"
        app:bulletGapWidth="8dp"
        app:bulletRadius="2dp"
        app:historyEnable="true"
        app:historySize="99"
        app:linkColor="#FF2196F3"
        app:linkUnderline="true"
        app:quoteCapWidth="2dp"
        app:quoteColor="#FF2196F3"
        app:quoteStripeWidth="8dp" />
</ScrollView>
```

In your `Activity`

```
RichEditText richEditText = (RichEditText) findViewById(R.id.rich_text);
richEditText.fromHtml(yourHtmlStr);
```

More example [click here](https://github.com/huzhenjie/RichTextEditor/blob/master/app/src/main/java/com/scrat/app/richtexteditor/MainActivity.java)


# Important

```
1.0.3 Glide version 4.3.0
1.0.2 Glide version 3.7.0
```

