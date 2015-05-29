package com.syw.weiyu.dao.shuoshuo;

import com.alibaba.fastjson.JSON;
import com.syw.weiyu.AppConstants;
import com.syw.weiyu.AppContext;
import com.syw.weiyu.AppException;
import com.syw.weiyu.api.Listener;
import com.syw.weiyu.bean.*;
import com.syw.weiyu.bean.jsonobj.NearbyShuoshuoItemJsonObj;
import com.syw.weiyu.bean.jsonobj.NearbyShuoshuoListJsonObj;
import com.syw.weiyu.dao.location.LocationDao;
import com.syw.weiyu.third.lbs.LBSCloud;
import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;
import net.tsz.afinal.http.AjaxParams;

import java.util.ArrayList;
import java.util.List;

/**
 * author: songyouwei
 * date: 2015-05-19
 * desc: 说说数据存取类
 */
public class ShuoshuoDao {

    /**
     * 获取附近的说说列表
     * @param pageIndex
     * @throws AppException
     */
    public void getNearByList(int pageIndex, final Listener<ShuoshuoList> listener) {
        String url = AppConstants.url_nearby_search;

        AjaxParams params = LBSCloud.getInitializedParams(AppConstants.geotable_id_shuoshuo);
        params.put("q","");
        params.put("location", new LocationDao().get().getLongitude() + "," + new LocationDao().get().getLatitude());
        params.put("tags","");
        params.put("radius",AppConstants.default_radius);
        //按时间|距离排序，优先显示时间靠前的
        params.put("sortby","timestamp:-1|distance:1");
        params.put("page_index",pageIndex+"");
        params.put("page_size",AppConstants.page_size_default);
        //get
        FinalHttp http = new FinalHttp();
        http.get(url, params, new AjaxCallBack<String>() {
            @Override
            public void onSuccess(String s) {
                try {
                    ShuoshuoList shuoshuoList = parseShuoshuosFromJson(s);
                    AppContext.put(AppContext.KEY_NEARBYSHUOSHUOS,shuoshuoList);
                    listener.onCallback(Listener.CallbackType.onSuccess, shuoshuoList, null);
                } catch (AppException e) {
                    listener.onCallback(Listener.CallbackType.onFailure, null, e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t, int errorNo, String strMsg) {
                listener.onCallback(Listener.CallbackType.onFailure, null, strMsg);
            }
        });
    }

    /**
     * 获取评论列表
     * @param ssId 说说ID
     * @return
     * @throws AppException
     */
    public void getComments(long ssId,final Listener<List<Comment>> listener) throws AppException {
        String url = AppConstants.url_list_poi;

        AjaxParams params = LBSCloud.getInitializedParams(AppConstants.geotable_id_comment);
        params.put("q","");
        //按时间|距离排序，优先显示时间靠前的
        params.put("sortby","timestamp:-1|distance:1");
        params.put("ssId",ssId+","+ssId);
        //get
        FinalHttp http = new FinalHttp();
        http.get(url, params, new AjaxCallBack<String>() {
            @Override
            public void onSuccess(String s) {
                super.onSuccess(s);
                try {
                    List<Comment> comments = parseCommentsFromJson(s);
                    listener.onCallback(Listener.CallbackType.onSuccess, comments, null);
                } catch (AppException e) {
                    listener.onCallback(Listener.CallbackType.onFailure, null, e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t, int errorNo, String strMsg) {
                super.onFailure(t, errorNo, strMsg);
                listener.onCallback(Listener.CallbackType.onFailure, null, strMsg);
            }
        });
    }

    /**
     * 发布说说
     * @param content
     * @param listener
     */
    public void add(String content, final Listener<String> listener) {
        String url = AppConstants.url_create_poi;

        AjaxParams params = LBSCloud.getInitializedParams(AppConstants.geotable_id_shuoshuo);
        //account info
        Account account = (Account) AppContext.get(AppContext.KEY_ACCOUNT);
        params.put("userId", account.getId());
        params.put("userName", account.getName());
        //location info
        MLocation location = (MLocation) AppContext.get(AppContext.KEY_LOCATION);
        params.put("longitude", location.getLongitude());
        params.put("latitude", location.getLatitude());
        //content
        params.put("content",content);
        //timestamp, same as create_time
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));

        //post
        new FinalHttp().post(url, params, new AjaxCallBack<String>() {
            @Override
            public void onSuccess(String s) {
                super.onSuccess(s);
                if (JSON.parseObject(s).getInteger("status") != 0) listener.onCallback(Listener.CallbackType.onSuccess,null,null);
                else listener.onCallback(Listener.CallbackType.onFailure,null,null);
            }

            @Override
            public void onFailure(Throwable t, int errorNo, String strMsg) {
                super.onFailure(t, errorNo, strMsg);
                listener.onCallback(Listener.CallbackType.onFailure,null,strMsg);
            }
        });
    }

    /**
     * 解析包含说说列表的json数据
     * eg:
     {
         "status": 0,
         "total": 35,
         "size": 1,
         "contents": [
             {
             "timestamp": 1432767399619,
             "userName": "魅力四射",
             "uid": 890849790,
             "province": "北京市",
             "geotable_id": 99489,
             "content": "微乐官方免费注册下载网址:http:\/\/www.v89.com 邀请码:18765068625\ 现在赚的就是互联网的钱，要懂得走在时代前沿。 欢迎你加入微乐 ， 注册好了进群学习交流 QQ群:274307262",
             "district": "东城区",
             "create_time": 1432767234,
             "city": "北京市",
             "userId": "865418028527404",
             "location": [
                 116.403874,
                 39.914889
             ],
             "title": null,
             "coord_type": 3,
             "type": 0,
             "distance": 1014058,
             "weight": 0
             }
         ]
     }
     * @param jsonStr
     * @return
     */
    private ShuoshuoList parseShuoshuosFromJson(String jsonStr) throws AppException {
        NearbyShuoshuoListJsonObj jsonObj = JSON.parseObject(jsonStr, NearbyShuoshuoListJsonObj.class);
        if (jsonObj.getStatus()!=0) throw new AppException("说说获取出错");
        int total = jsonObj.getTotal();
        List<Shuoshuo> list = new ArrayList<>();
        for (int i=0;i<jsonObj.getSize();i++) {
            Shuoshuo shuoshuo = new Shuoshuo();
            NearbyShuoshuoItemJsonObj poi = jsonObj.getContents().get(i);
            shuoshuo.setId(poi.getId());
            shuoshuo.setTimestamp(poi.getTimestamp());
            shuoshuo.setUserId(poi.getUserId());
            shuoshuo.setUserName(poi.getUserName());
            shuoshuo.setContent(poi.getContent());
            shuoshuo.setLocation(new MLocation(poi.getCity(),poi.getProvince(),poi.getDistrict()));
            list.add(shuoshuo);
        }
        return new ShuoshuoList(total,list);
    }

    /**
     * eg:
     *
       {
         "status": 0,
         "size": 1,
         "total": 1,
         "pois": [
             {
             "title": null,
             "location": [
                     1,
                     1
                ],
             "city": "",
             "create_time": "2015-05-20 15:37:16",
             "geotable_id": 104953,
             "province": "",
             "district": "",
             "ssId": 111,
             "userId": "357143040902071",
             "city_id": 0,
             "id": 870430815
             }
         ],
         "message": "成功"
       }
     * @param jsonStr
     * @return
     */
    private List<Comment> parseCommentsFromJson(String jsonStr) throws AppException {
        NearbyShuoshuoListJsonObj jsonObj = JSON.parseObject(jsonStr,NearbyShuoshuoListJsonObj.class);
        if (jsonObj.getStatus()!=0) throw new AppException("评论获取出错");
        List<Comment> list = new ArrayList<>();
        for (int i=0;i<jsonObj.getSize();i++) {
            Comment comment = new Comment();
            NearbyShuoshuoItemJsonObj poi = jsonObj.getContents().get(i);
            comment.setUserId(poi.getUserId());
            comment.setUserName(poi.getUserName());
            comment.setTimestamp(poi.getTimestamp());
            comment.setContent(poi.getContent());
            list.add(comment);
        }
        return list;
    }
}
