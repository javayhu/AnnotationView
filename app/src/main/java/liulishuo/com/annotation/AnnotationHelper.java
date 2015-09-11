package liulishuo.com.annotation;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 单例模式的用来处理文本和它对应的标注内容的工具类
 *
 * @author hujiawei 15/9/10
 */
public class AnnotationHelper {

    private static final String TAG = AnnotationHelper.class.getSimpleName();

    //当然，为了更好的扩展，最好是将该内容放置在文件中，例如在assets中建立annotations.txt文件，方便扩展和修改
    private String mAnnotationDetails = "[\n" +
            "            [{text: '天', ipa: 'おは', ipa_visible: true }, {text: '気', ipa: 'き', ipa_visible: true}],\n" +
            "            [{text: '予', ipa: 'よ', ipa_visible: true}, {text: '報', ipa: 'ほう', ipa_visible: true}],\n" +
            "            [{text: 'によ', ipa: 'によ', ipa_visible: false},    {text: 'る', ipa: 'る', ipa_visible: false}],\n" +
            "            [{text: 'と', ipa: 'と', ipa_visible: false}] ]";

    private Map<String, AnnotationItem> mAnnotationMap;

    private static class AnnotationHelperHolder {//以静态内部类的方式实现单例
        static AnnotationHelper instance = new AnnotationHelper();
    }

    public static AnnotationHelper getInstance() {
        return AnnotationHelperHolder.instance;
    }

    private AnnotationHelper() {
        mAnnotationMap = new HashMap<String, AnnotationItem>();
        init();
    }

    //初始化，将details读取出来放到map中
    private void init() {
        try {
            JSONArray jsonArray = new JSONArray(mAnnotationDetails);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray subArray = jsonArray.getJSONArray(i);//一定是 JSONArray
                for (int j = 0; j < subArray.length(); j++) {
                    JSONObject jsonObject = subArray.getJSONObject(j);//一定是 JSONObject
                    AnnotationItem item = new AnnotationItem(jsonObject);
                    mAnnotationMap.put(item.text, item);
                }
            }
            Log.e(TAG, mAnnotationMap.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回给定文本的标注内容
     */
    public AnnotationItem getAnnotationText(String source) {
        return mAnnotationMap.get(source);
    }

    /**
     * 标注item
     */
    static class AnnotationItem {
        String text = "";
        String ipa = "";
        boolean visible = false;

        public AnnotationItem(JSONObject jsonObject) {
            text = jsonObject.optString("text");
            ipa = jsonObject.optString("ipa");
            visible = jsonObject.optBoolean("ipa_visible");
        }
    }

}
