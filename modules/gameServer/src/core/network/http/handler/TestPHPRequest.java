package core.network.http.handler;


import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.ZleData_Result;

public class TestPHPRequest {

    @RequestMapping(uri = "/game/test/index")
    public void index(HttpRequest request, HttpResponse response) {
        response.response(ZleData_Result.make(ErrorCode.Success, "TestPHPRequest"));
    }
}
