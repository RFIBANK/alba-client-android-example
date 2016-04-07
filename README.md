Демонстрационное Android приложение
=============

Пример работы с Alba используюя библиотеку: https://github.com/RFIBANK/alba-client-java

Приложение позволяет иницировать транзакцию, а затем проверяет её статус.

Для корректной работы необходимо прописать ключ в файле strings.xml:

    <integer name="alba_service_id">МЕСТО ДЛЯ ID СЕРВИСА</integer>
    <string name="alba_key">МЕСТО ДЛЯ КЛЮЧА</string>
