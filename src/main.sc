require: patterns.sc
require: scripts/funcYou.js
require: scripts/checkCustomAnsToGo.js
require: scripts/getRandomEl.js

require: slotfilling/slotFilling.sc
  module = sys.zb-common
  
  

theme: /BullsAndCowsGame
# Заходим в игру по приветствию или по action start
    state: Start
        q!: $regexp</start>
        intent!: /привет
# Всстроеенная функция проставления заглавных букв        
        script:
            $temp.botName = capitalize($injector.botName);
        random:
# Используем имя прописанное через встроенную переменную injector в yaml
# funcYou - функция, которая заменяет Ты на Вы
            a: Привет. Я Бот {{$temp.botName}}. Умею играть в Быки и коровы. Сыграем?
            a: Приветствую. Меня зовут {{$temp.botName}}. Я могу поиграть с {{funcYou("Вами", "вами", "тобой")}} Быки и коровы. Сыграем?
# Ловим согласие поиграть          
        state: ReactYes
            q: {[$beginningWords] [$interjections]} ($yes/$yesAgreeTo/$certainly/$want) {[$please] [$interjections]}
            q: {[$beginningWords] [$interjections]} (игра*/сыгра*) [$please] [$interjections]
            q: * ($yes/$yesAgreeTo/$certainly) давай (игра*/сыгра*) *
            script:
                checkCustomAnsToGo();
            go!: /BullsAndCowsGame/BullsAndCows
# Ловим отказ от поиграть             
        state: ReactNo
            q: $no [$thanks]
            go!: /BullsAndCowsGame/GameOn/react_Stop
# переходим в подтверждение    
    state: BullsAndCows
        q!: BullsAndCows
        script:
            $client.isFirstGame = true;
        go!: /BullsAndCowsGame/GameOn
# начинаем игру по подтверждению 
    state: GameOn || modal = true
        if: $client.isFirstGame == true
            a: Хорошо. {{funcYou("Давайте", "Давайте", "Давай")}} начнём!
            a: Я загадаю четырехзначное число, все цифры которого будут различны, а затем буду давать {{funcYou("Вам", "вам", "тебе")}} подсказки, считая количество быков и коров в {{funcYou("Ваших", "ваших", "твоих")}} предположениях. \nБык означает, что какая-то из цифр стоит на своём месте; \nкорова значит, что в моём числе эта цифра есть, но занимает другую позицию.
            a: Итак, я загадал, {{funcYou("попробуйте", "попробуйте", "попробуй")}} угадать!
        else:
            a: Отлично, {{funcYou("давайте", "давайте", "давай")}} сыграем ещё раз! \nЯ загадал следующее число, {{funcYou("попробуйте", "попробуйте", "попробуй")}} угадать!

        script:
            $client.isFirstGame = false;
            $client.numberToGuess = "";
# формируем массив и собираем набор цифр, который будем отгадывать
# arr.indexOf(n) вернёт либо индекс найденного элемента n, либо -1, если элемента n в массиве нет. функция arr.splice(index, 1) удалит из массива arr 1 элемент начиная с позиции index. (если index окажется отрицательным, то отступ будет отсчитываться с конца массива) Также помимо изменения массива arr, функция splice вернет массив из удаленных элементов.
            var arr = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"];
            for (var i = 0; i < 4; i++) {
                var n = getRandomEl(arr);
                $client.numberToGuess += n;
                arr.splice(arr.indexOf(n), 1);
            }
            $client.numberOfTries = 0;
            $client.incorrectNumbersCount = 0;
# ловим цифры 
        state: react_Number
            q: $guessNumber * $weight<+2>
            script:
                $client.lastEnteredNumber = $parseTree._guessNumber;
# проходим циклом по цифрам введенного числа, определяем, если есть неправильные обновляем счетчик попыток
                for (var i = 0; i < 4; i++) {
                    if ($client.lastEnteredNumber.split("").filter(function(el, index){return el != $client.lastEnteredNumber.charAt(i)}).join("").length < 3) {
                        $temp.invalidNumb = true;
                        $client.incorrectNumbersCount++;
                        break;
                    }
                }
# считаем количество правильных быков и коров в ответе
                if (!$temp.invalidNumb) {
                    $client.incorrectNumbersCount = 0;
                    $client.bulls = 0;
                    $client.cows = 0;
# проходим циклом по цифрам введенного числа, определяем, если есть правильные обновляем счетчик быков и коров
                    for (var i = 0; i < 4; i++) {
                        if ($client.lastEnteredNumber.charAt(i) == $client.numberToGuess.charAt(i)) {
                            $client.bulls++;
                        }
                        else if ($client.numberToGuess.indexOf($client.lastEnteredNumber.charAt(i)) > -1) {
                            $client.cows++;
                        }
                    }
                    $client.numberOfTries++;
                }
# обработка ввода меньше 4 цифр
            if: $temp.invalidNumb && $client.incorrectNumbersCount > 2
                go!: /BullsAndCowsGame/MoreThanTwoIncorrectNumbers
            if: $temp.invalidNumb
                a: Так нечестно! Цифры в {{funcYou("Вашем", "вашем", "твоем")}} числе не должны повторяться! {{funcYou("Попробуйте", "Попробуйте", "Попробуй")}} ещё!
                go: /BullsAndCowsGame/GameOn/react_Number
            if: $parseTree._guessNumber == $client.numberToGuess
                a: 4 быка! Ура! {{funcYou("Вы", "Вы", "Ты")}} {{funcYou("угадали", "угадали", "угадал")}} число c {{$client.numberOfTries}}-ой попытки!\n{{funcYou("Хотите", "Хотите", "Хочешь")}} сыграть ещё раз?
                go!: /BullsAndCowsGame/WannaPlayAgain
            a: Сейчас у {{funcYou("Вас", "вас", "тебя")}} {{$client.cows}} {{$nlp.conform("корова", $client.cows)}}, {{$client.bulls}} {{$nlp.conform("бык", $client.bulls)}}. \nЭто {{funcYou("Ваша", "ваша", "твоя")}} {{$client.numberOfTries}}-ая попытка. {{funcYou("Попробуйте", "Попробуйте", "Попробуй")}} ещё!
            go: /BullsAndCowsGame/GameOn/react_Number
# обработка ввода меньше 4 цифр
        state: react_IncorrectNumber
            q: $incorrectNumber * $weight<+2>
            script:
                $client.incorrectNumbersCount++;
            if: $client.incorrectNumbersCount <= 2
                a: Такое число нам не подходит. {{funcYou("Назовите", "Назовите", "Назови")}} четырехзначное число. \nНе {{funcYou("забывайте", "забывайте", "забывай")}}, что цифры не должны повторяться.
                go: /BullsAndCowsGame/GameOn/react_Number
            else:
                go!: /BullsAndCowsGame/MoreThanTwoIncorrectNumbers
# реакция на "как играть"
        state: react_HowToPlay
            q: * {$howSyns * играть} *
            q: * [какие] правил* *
            q: * правил* игр* *
            a: Мы с {{funcYou("Вами", "вами", "тобой")}} играем в "Быки и коровы".
            a: Я загадал четырехзначное число, все цифры которого различны, а {{funcYou("Вы", "вы", "ты")}} {{funcYou("должны", "должны", "должен")}} попытаться угадать это число. \nЯ буду давать {{funcYou("Вам", "вам", "тебе")}} подсказки, считая количество быков и коров в {{funcYou("Ваших", "ваших", "твоих")}} предположениях. \nБык означает, что какая-то из цифр стоит на своём месте; \nкорова значит, что в моём числе эта цифра есть, но занимает другую позицию.

            if: $client.numberOfTries>0
                a: В числе {{$client.lastEnteredNumber}} {{$client.cows}} {{$nlp.conform("корова", $client.cows)}}, {{$client.bulls}} {{$nlp.conform("бык", $client.bulls)}}. \nЭто была {{funcYou("Ваша", "ваша", "твоя")}} {{$client.numberOfTries}}-ая попытка. {{funcYou("Попробуйте", "Попробуйте", "Попробуй")}} ещё!
            else:
                a: {{funcYou("Назовите", "Назовите", "Назови")}} любое четырехзначное число. Не {{funcYou("забывайте", "забывайте", "забывай")}}, что цифры не должны повторяться.
            go: /BullsAndCowsGame/GameOn/react_Number
# реакция на "стоп"
        state: react_Stop
            q: (стоп/stop/хватит/надоел*/закончить/заканчивай)
            q: * (выйти) *
            q: * (назад) *
            q: * (хватит) *
            q: * (не хочу) *
            q: * (надоел*) *
            a: Хорошо, {{funcYou("давайте", "давайте", "давай")}} закончим. Это было число {{$client.numberToGuess}}.
            go!: /techEndOfTheme
# реакция на "сдаюсь"        
        state: react_WannaGiveUp
            q: * $giveUp *
            a: {{funcYou("Вы", "Вы", "Ты")}} действительно {{funcYou("хотите", "хотите", "хочешь")}} сдаться?
            buttons:
                "ДА" -> /BullsAndCowsGame/GiveUp
            buttons:
                "НЕТ" -> /BullsAndCowsGame/Continue
# дублирование кнопок текстом
            state: react_Yes
                q: ($yes/$want)
                go!: /BullsAndCowsGame/GiveUp

            state: react_No
                q: $no
                go!: /BullsAndCowsGame/Continue
# реакция на "я не знаю"
        state: IDontKnow
            q: * $knowNot *
            q: [$beginningWords] $no
            a: {{funcYou("Сдаётесь?", "Сдаётесь?", "Сдаёшься?")}}
            buttons:
                "Сдаюсь" -> /BullsAndCowsGame/GiveUp
            buttons:
                "Нет" -> /BullsAndCowsGame/Continue
# прододжение игры, если пользователь передумал прерывать игру
    state: Continue
        a: Отлично! {{funcYou("Попробуйте", "Попробуйте", "Попробуй")}} угадать моё число! Не {{funcYou("забывайте", "забывайте", "забывай")}}, что цифры не должны повторяться.
        go: /BullsAndCowsGame/GameOn/react_Number
# вывод загаданного числа, если пользователь сдался
    state: GiveUp
        a: Это было число {{$client.numberToGuess}}. \n{{funcYou("Хотите", "Хотите", "Хочешь")}} сыграть ещё раз?
        go!: /BullsAndCowsGame/WannaPlayAgain
# обработка многократного неправильного ввода
    state: MoreThanTwoIncorrectNumbers
        script:
            $client.incorrectNumbersCount = 0;
        a: {{funcYou("Cдаётесь?", "Cдаётесь?", "Cдаёшься?")}} Или, может быть, напомнить {{funcYou("Вам", "вам", "тебе")}} правила?
        buttons:
            "Продолжить" -> /BullsAndCowsGame/Continue
        buttons:
            "Напомнить правила" -> /BullsAndCowsGame/GameOn/react_HowToPlay
        buttons:
            "Сдаюсь" -> /BullsAndCowsGame/GiveUp
# перевод в состояние сыграть еще после выигрыша и после того как сдался
    state: WannaPlayAgain
        buttons:
            "Играть еще раз" -> /BullsAndCowsGame/GameOn
        buttons:
            "Спасибо!" -> /BullsAndCowsGame/Thanks

        state: react_Yes
            q: ($yes/$lets/можно/давай [еще])
            go: /BullsAndCowsGame/GameOn

        state: react_No
            q: $no [$thanks]
            go!: /BullsAndCowsGame/GameOn/react_Stop
# реакция на "спасибо"     
    state: Thanks
        a: Всегда пожалуйста. \nЕсли {{funcYou("Хотите", "Хотите", "Хочешь")}} сыграть ещё раз жмите на кнопку старт
        buttons:
            "Старт" -> /BullsAndCowsGame/Start
# реакция на "пока"                  
    state: Bye
        q: пока
        a: Пока пока
# реакция на "стоп"         
    state: Stop
        q: $no [$thanks]
        go!: /BullsAndCowsGame/GameOn/react_Stop
# реакция на неучтенные типы запросов
    state: NoMatch
        event!: noMatch
        random: 
            a: Извините, я не понял, что {{funcYou("Вы", "Вы", "Ты")}} {{funcYou("сказали", "сказали", "сказал")}}.
            a: Простите. я не распознал {{funcYou("Вашу", "вашу", "твою")}} фразу.
        random: 
            a: Переформулируйте, пожалуйста.
            a: Скажите по-другому.