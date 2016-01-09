package top.liborange.wechatlucky;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by liborange on 2016/1/9 下午1:36.
 * desc: 微信抢红包类
 */
public class EasyLuckyMoney extends AccessibilityService {
    private static final String TAG = "EasyLuckyMoney";
    
    //拆红包类
    private static final String RECEIVE_CLASS = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    //微信主界面，聊天界面
    private static final String LAUNCHER_CLASS = "com.tencent.mm.ui.LauncherUI";
    //微信红包详情类
    private static final String DETAIL_CLASS = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    private List<String> received = new ArrayList<String >();
    private String tempID = null;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
               //通知栏有新消息提醒时。
                openNotificationInfo(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                String ClassName = event.getClassName().toString();

                if (ClassName.equals(RECEIVE_CLASS)) {
                    //进入「拆红包」界面
                    receiveLucky();
                } else if (ClassName.equals(LAUNCHER_CLASS)||ClassName.equals("android.widget.ListView")) {
                    //进入「微信聊天」界面
                    touchLucky();
                }else if (ClassName.equals(DETAIL_CLASS)) {
                    if (tempID != null) {
                        received.add(tempID);
                        Log.i(TAG + "红包详情：", "已经拆开过" + tempID+"了");
                        tempID = null;
                        performGlobalAction(GLOBAL_ACTION_BACK);
                    }
                }else{

                }
                break;

        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void receiveLucky() {
        AccessibilityNodeInfo node = getRootInActiveWindow();
        if (node != null) {
            List<AccessibilityNodeInfo> childs = node.findAccessibilityNodeInfosByText("拆红包");
            Log.i(TAG, childs.size() + "");
            for (AccessibilityNodeInfo child : childs) {
                if (child.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    received.add(tempID);
                    Log.i(TAG + "拆红包", "成功，抢到红包" + tempID);
                    tempID = null;
                }
            }
            if(node.findAccessibilityNodeInfosByText("超过1天未领取，红包已失效").size()>0) {
                if(tempID!=null)
                    received.add(tempID);
                tempID = null;
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        } else {
            Log.i(TAG, "node = null");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void touchLucky() {
        List<AccessibilityNodeInfo> touchNodes = getRootInActiveWindow().findAccessibilityNodeInfosByText("领取红包");
        for (AccessibilityNodeInfo touch : touchNodes) {
            touch.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            AccessibilityNodeInfo parent = touch.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    String id = calNodeID(parent);
                    if(!received.contains(id)) {
                        Log.i(TAG + "领取红包", "红包ID：" + id);
                        if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                            tempID = id;
                    }
                    break;
                }
                parent = parent.getParent();
            }
        }
    }

    /**
     * 检查通知栏消息是否有红包，有了就点击进入
     * @param event
     */
    private void openNotificationInfo(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        for (CharSequence text : texts) {
            String value = text.toString();
            Log.i(TAG, "通知栏消息: " + value);
            if (value.contains("[微信红包]")) {
                //打开通知栏中包含微信红包的消息
                Log.i(TAG+"高能预警", "有红包");
                if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                    Notification notification = (Notification) event.getParcelableData();
                    PendingIntent intent = notification.contentIntent;
                    try {
                        intent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 以红包描述加红包默认id作为该红包的ID
     * @param nodeInfo
     * @return
     */
    private String calNodeID(AccessibilityNodeInfo nodeInfo) {
        String content;
        String string = nodeInfo.findAccessibilityNodeInfosByText("领取红包").get(0).getParent().getChild(0).getText().toString();
        Pattern objHashPattern = Pattern.compile("(?<=@)[0-9|a-z]+(?=;)");
        Matcher objHashMatcher = objHashPattern.matcher(nodeInfo.toString());

        // AccessibilityNodeInfo必然有且只有一次匹配，因此不再作判断
        objHashMatcher.find();

        return string+"@"+objHashMatcher.group(0);
    }

    @Override
    public void onInterrupt() {

    }
}