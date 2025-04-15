package com.luckyframework.httpclient.generalapi.plugin;

import com.luckyframework.common.Table;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.proxy.logging.FontUtil;

public class DefaultTimeStatisticsHandle implements TimeStatisticsHandle {

    @Override
    public void handle(TimeStatisticsInfo info) throws Exception {
        String tag = FontUtil.getBackGreenStr("NORMAL");
        String time = UnitUtils.millisToTime(info.getTimeConsuming());
        String method = info.getExecuteMeta().getMetaContext().getSimpleSignature();

        String _method = FontUtil.getGreenUnderline(method);
        String _time = FontUtil.getGreenStr(time);
        if (info.isWarn()) {
            tag = FontUtil.getBackYellowStr(" WARN ");
            _time = FontUtil.getYellowStr(time);
            _method = FontUtil.getYellowUnderline(method);
        }
        if (info.isSlow()) {
            tag = FontUtil.getBackRedStr(" SLOW ");
            _time = FontUtil.getRedStr(time);
            _method = FontUtil.getRedUnderline(method);
        }

        System.out.printf("%s  %s method takes %s\n", tag, _method, _time);

    }


}
