import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) {
        // Создаем объект класса SLAU
        SLAU slau = new SLAU();

        // Вызываем метод init с переданным именем файла для заполнения матрицы matrix из файла.
        // Если из метода init выброшено исключение FileNotFoundException, значит файла с таким
        // именем нет, выводим сообщение и завершаем программу.
        try {
            slau.init("data.txt");
        } catch (FileNotFoundException e) {
            System.out.println("Файла не существует");
            return;
        }

        // Выводим пояснительное сообщение
        System.out.println("Матрица, прочитанная из файла: ");
        // Вызываем метод format() класса SLAU, который представляет матрицу в виде форматированной строки
        // (см. описание метода format() класса SLAU). Результат печатаем на экран
        System.out.println(slau.format());

        if (slau.prepareMatrix()) {
            System.out.println("ДУС выполняется");
            // Вызываем метод solve() класса SLAU, который запускает алгоритм Гаусса-Зейделя с флагом dus == true,
            // т.е. для случая, когда ДУС выполняется. Результаты работы алгоритма содержатся в полях класса SLAU
            slau.solve(true);
        } else {
            if (slau.getStatus().equals("")) {
                System.out.println("ДУС не выполняется");
                // Вызываем метод solve() класса SLAU, который запускает алгоритм Гаусса-Зейделя с флагом dus == false,
                // т.е. для случая, когда ДУС не выполняется. Результаты работы алгоритма содержатся в полях класса SLAU
                // Ограничение количества операций - в статическом поле класса SLAU
                slau.solve(false);

                // Проверяем, что решение найдено. Если оно было не найдено из-за расходимости метода, поле status
                // класса SLAU будет содержать пояснительное сообщение
                if (!slau.getStatus().equals("")) {
                    // Выводим сообщение на экран и завершаем выполнение программы, путем возврата из функции main
                    System.out.println(slau.getStatus());
                    return;
                }
            } else {
                System.out.println(slau.getStatus());
                return;
            }
        }

        // Получаем строку, которая содержит форматированное представление массива найденных неизвестных
        // и выводим ее на экран
        String solutions = slau.getSolutions();
        System.out.println("Решения:");
        System.out.println(solutions);
    }
}
