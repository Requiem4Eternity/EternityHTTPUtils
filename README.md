#注意：此项目因使用过时的HTTPUrlConnection方式，现已废弃并停止维护，代码仅供学习交流！
# EternityHTTPUtils
闲来无事自己写的安卓HTTP框架恩
特点：1、没有任务队列，即用即创建
     2、小，很小，非常小！！！
使用方法大概是这样：

* 连接并获取JSON返回值：

GET:

        new EternityHTTPUtil.connForJSON(context, url, EternityHTTPUtil.GET, null, new IJSONCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                //恩这里的json就是服务器返回的JSON数据
            }

            @Override
            public void onError(int reason) {
                //这里是服务器出错时的回调方法
            }

            @Override
            public void onNetworkCrashed() {
                //这里是网络连接出现问题时的回调方法
            }
        });
        
        
POST:
        
        HashMap<String,String> hm=new HashMap<>();
        //↑这个HashMap里的数据会自动打成表单提交的参数恩
        hm.put("key","value");
        new EternityHTTPUtil().connForJSON(context, url, EternityHTTPUtil.POST, hm, new IJSONCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                
            }

            @Override
            public void onError(int reason) {

            }

            @Override
            public void onNetworkCrashed() {

            }
        });

  当然如果你不想写出错和网络连接出现问题的回调事件的话：

            @Override
            public void onError(int reason) {
                EHUErrorHandler.onError(context, reason);
            }
            
            @Override
            public void onNetworkCrashed() {
                EHUErrorHandler.onNetworkError(context);
            }
            
  作用是吐司你的错误信息。
 
 
* 普通连接（返回字符串）：
 
        new EternityHTTPUtil().conn(context, url, EternityHTTPUtil.POST, hm, new IConnCallBack() {
            @Override
            public void onSuccess(String str) {
                
            }

            @Override
            public void onError(int reason) {

            }

            @Override
            public void onNetworkCrashed() {

            }
        });
        
* 如果你需要单例模式：
 
        EternityHTTPUtil eternityHTTPUtil=new EternityHTTPUtil();
        //然后用eternityHTTPUtil对象做你的操作就好了
        //注意：下次连接会放弃上一次的连接
        
        //不用的时候：
        eternityHTTPUtil.shutDown();
        //↑自动断开连接，关闭流
        
* 获取HttpURLConnection对象：
 
        final EternityHTTPUtil ehu=new EternityHTTPUtil();
        ehu.createConn(url, EternityHTTPUtil.GET, hashMap, new IConnCreateCallback() {
            @Override
            public void onConnCreated(HttpURLConnection connection) {
                //这里的connection就是已经写入hashMap中参数的HttpURLConnection对象了
                //Do what ever u want
                //最后连接！
                ehu.startConn(context,connection,callBack);
                //如果你希望返回JSON
                ehu.startConnForJSON(context,connection,JSONCallBack);
            }
        });
 
* 文件下载：

        new EHUDownLoadUtil().download(activity,"下载地址","下载路径",new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case EHUDownLoadUtil.DOWNLOADED:
                        //下载完成了！
                        break;
                    case EHUDownLoadUtil.DOWNLOADING:
                        int process= (int) msg.obj;
                        //↑这个是下载进度，0~100
                        break;
                    case EHUDownLoadUtil.PERMISSION_DENIED:
                        //获取写入权限失败的回调
                        break;
                }
            }
        });
        
是不是超级简单呢~
