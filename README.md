# AlipayCallback
利用支付宝web版接口，线程实时回调，websocket 实时传送 订单 json，达到固码免签约个人支付方案。

本 repo 只是一个模块，具体业务请自行实现。 另外当 cookie 过期失效的时候，会自动停止线程，并利用 server酱 发送微消息通知开发者


# 使用方法

登录支付宝网页版，获取 cookie 与 userid 替换到 applicatio.properties 运行即可
