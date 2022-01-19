function funcYou(text4Vy, text4vy, text4ty) {
    var _fnName = 'funcYou',
        client  = $jsapi.context().client;
        
    if (!text4Vy || !text4vy || !text4ty) {
        $reactions.answer("[JS-ERROR] " + _fnName + ": не указаны обязательные параметры");
        return false;
    }
        
    return (client.you == "Вы") ? text4Vy : (client.you == "вы") ? text4vy : text4ty;
}