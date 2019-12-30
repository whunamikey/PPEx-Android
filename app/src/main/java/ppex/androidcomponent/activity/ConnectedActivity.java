package ppex.androidcomponent.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import ppex.androidcomponent.adapter.FilesAdapter;
import ppex.androidcomponent.busevent.BusEvent;
import ppex.androidcomponent.handler.client.RequestClient;
import ppex.client.Client;
import ppex.client.R;
import ppex.proto.entity.testpack.Files;
import ppex.proto.entity.through.Connect;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;

public class ConnectedActivity extends Activity {

    private static String TAG = ConnectedActivity.class.getName();

    private ListView lv_showfiles;
    private Button btn_getfiles;
    private TextView tv_back,tv_title;
    private Button btn_send;
    private EditText et_content;

    private int connectType;
    private File sdFile;
    private File targetFile;

    private FilesAdapter filesAdapter;
    private List<Files> files;
    private Client client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_connected);
        EventBus.getDefault().register(this);
        getIntentValue();
        findIds();
        setListener();
        initComponent();
        setRequestHandler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void getIntentValue(){
        connectType = Client.getInstance().getConnType2Target().ordinal();
        Log.e(TAG,"connectype is :" + connectType);
        RequestClient.getDefault();
        sdFile = Environment.getExternalStorageDirectory();
        targetFile = new File(sdFile.getAbsolutePath(),"test.pdf");
    }

    private void findIds(){
        lv_showfiles = findViewById(R.id.lv_connected_showfiles);
        btn_getfiles = findViewById(R.id.btn_connected_getfiles);
        tv_back = findViewById(R.id.tv_connected_back);
        tv_title = findViewById(R.id.tv_connected_title);
        btn_send = findViewById(R.id.btn_connected_send);
        et_content = findViewById(R.id.et_connected_content);

        tv_title.setText(Client.getInstance().getConnTarget().getMacAddress());
    }

    private void setListener(){
        tv_back.setOnClickListener(v ->{
            this.finish();
        });
        btn_getfiles.setOnClickListener(v ->{
            //请求获取目录
            Map<String,String> params = new HashMap<>();
            params.put("path","root");
            RequestClient.getDefault().sendRequest("/file/getfiles",null,params);
        });
        btn_send.setOnClickListener(v ->{
            String content = et_content.getText().toString();
            TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
            txtTypeMsg.setFrom(Client.getInstance().getAddrLocal());
            txtTypeMsg.setTo(Client.getInstance().getConnTarget().getAddress());
            txtTypeMsg.setReq(true);
            txtTypeMsg.setContent(content);
            if (Client.getInstance().getConnType2Target() == Connect.TYPE.FORWARD){
                RudpPack rudpPack = Client.getInstance().getAddrManager().get(Client.getInstance().getAddrServer1());
                rudpPack.send2(MessageUtil.txtmsg2Msg(txtTypeMsg));
            }else{
                RudpPack rudpPack = Client.getInstance().getAddrManager().get(Client.getInstance().getConnTarget().getAddress());
                rudpPack.send2(MessageUtil.txtmsg2Msg(txtTypeMsg));
            }
//            if(connectType == Connect.TYPE.FORWARD.ordinal()){
//                RudpPack rudpPack = Client.getInstance().addrManager.get(Client.getInstance().SERVER1);
//                rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
//            }else{
//                RudpPack rudpPack = Client.getInstance().addrManager.get(Client.getInstance().connectedMaps.get(0).getConnections().get(1).getAddress());
//                rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
//            }
        });
        lv_showfiles.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Map<String,String> params = new HashMap<>();
                params.put("path",files.get(i).getName());
                RequestClient.getDefault().sendRequest("/file/download",null,params);
                return true;
            }
        });
    }

    private void initComponent(){
        client = Client.getInstance();
        files = new ArrayList<>();
        filesAdapter = new FilesAdapter(files,this);
        lv_showfiles.setAdapter(filesAdapter);
    }

    private void setRequestHandler(){
        RequestClient.getDefault().setClient(client);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void handleMainThreadEvent(BusEvent event) {
        switch (BusEvent.Type.getByValue(event.getType())) {
            case FILE_GETFILES:
                Log.e(TAG,TAG+"get files response:" + event.getData());
                String fileStr = (String) event.getData();
                files= JSON.parseArray(fileStr, Files.class);
                filesAdapter.setFiles(files);
                filesAdapter.notifyDataSetChanged();
                break;
        }
    }


}
