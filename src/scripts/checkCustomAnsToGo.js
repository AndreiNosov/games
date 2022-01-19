function checkCustomAnsToGo() {
    var $context = $jsapi.context();

    if (typeof customAnswerList !== 'undefined' && !isEmpty(customAnswerList) && customAnswerList[$context.currentState]) {
        $reactions.transition(customAnswerList[$context.currentState]);
        return false;
    }

}