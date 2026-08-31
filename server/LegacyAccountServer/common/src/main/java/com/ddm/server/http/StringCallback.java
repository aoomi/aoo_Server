//-------------------------------------------------
// Litchi Game Server Framework
// Copyright(c) 2019 phantaci <phantacix@qq.com>
// MIT Licensed
//-------------------------------------------------
package com.ddm.server.http;

public interface StringCallback {

	void completed(String content);

	void failed(Exception ex);

}
