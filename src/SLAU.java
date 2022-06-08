import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SLAU {
    // EPSILON - погрешность при сравнении чисел
    private static final double EPSILON = 0.00000001;
    private static final int MAX_ITERATIONS_AMOUNT = 10;
    // matrix хранит в себе матрицу коэффициентов и свободных членов
    private double[][] matrix;
    // n - количество уравнений в системе
    private int n;
    // m - количество коэффициентов + 1 (свободные члены)
    private int m;
    // precision - требуемая точность
    private double precision;
    // solutions хранит в себе решение системы (если оно есть)
    private double[] solutions;
    // currentIteration хранит в себе приближения решений, получаемые на текущей итерации
    private double[] currentIteration;
    // status - информационная переменная, которая содержит одно из сообщений:
    // 1. "Невозможно решить итерационным методом"
    // 2. "Метод расходится"
    // Инициализируем пустой строкой, чтобы избежать NullPointerException
    private String status = "";
    // nonZeroTemplate - хранит в себе первую перестановку, полученную во время выполнения prepareMatrix(),
    // для которой на главной диагонали нет ни одного нолю
    private int[] nonZeroTemplate;


    // Метод isAEqualToB() проверяет два числа на равенство в пределах погрешности EPSILON
    private boolean isAEqualToB(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    // метод init читает данные из файла и заполняет matrix, n, m
    public void init(String pathToFile) throws FileNotFoundException {
        // Создаем Scanner и открываем файл по пути pathToFile на чтение. Если файла не существует, будет выброшено
        // исключение FileNotFoundException
        Scanner scanner = new Scanner(new FileInputStream(pathToFile));
        // Читаем первую строчку из файла, содержащую размеры n и m.
        readSizes(scanner);

        // Читаем очередную строку из файла
        String line = scanner.nextLine();
        // Создаем регулярное выражение (Pattern), которое может распознавать дробные числа со знаком.
        // В качестве разделителя используется точка
        Pattern pattern = Pattern.compile("-?\\d+\\.\\d+");
        // Получаем объект matcher из Pattern, который непосредственно находит дробные числа в строке line
        Matcher matcher = pattern.matcher(line);
        // Находим следующее дробное число
        matcher.find();
        // Вырезаем из строки line дробное число, находящееся в строке между индексами
        // matcher.start() и matcher.end(), после чего приводим вырезанную строку к типу double
        precision = Double.parseDouble(line.substring(matcher.start(), matcher.end()));

        // Инициализируем матрицу matrix.
        matrix = new double[n][];
        // Заполняем матрицу matrix, построчно читая строки коэффициентов и свободных членов
        for (int i = 0; i < n; i++) {
            matrix[i] = readEquation(scanner);
        }

        // Закрываем файл
        scanner.close();
    }

    // приватный метод readSizes служит для упрощения чтения кода, читает n и m со Scanner при помощи регулярного
    // выражения
    private void readSizes(Scanner scanner) {
        // Читаем очередную строку из файла
        String line = scanner.nextLine();
        // Создаем регулярное выражение (Pattern), которое может распознавать целые числа
        Pattern pattern = Pattern.compile("\\d+");
        // Получаем объект matcher из Pattern, который непосредственно находит целые числа в строке line
        Matcher matcher = pattern.matcher(line);
        // Находим следующее целое число
        matcher.find();
        // Вырезаем из строки line целое число, находящееся в строке между индексами matcher.start() и matcher.end(),
        // после чего приводим вырезанную строку к типу int
        n = Integer.parseInt(line.substring(matcher.start(), matcher.end()));
        // Находим следующее целое число
        matcher.find();
        // Вырезаем из строки line целое число, находящееся в строке между индексами matcher.start() и matcher.end(),
        // после чего приводим вырезанную строку к типу int
        m = Integer.parseInt(line.substring(matcher.start(), matcher.end()));
    }

    // приватный метод readEquation служит для упрощения чтения кода, читает одну строку коэффициентов и свободных член,
    // соответствующие одному уравнение системы. Использует Scanner и регулярное выражение.
    private double[] readEquation(Scanner scanner) {
        // Читаем очередную строку из файла
        String line = scanner.nextLine();
        // Создаем регулярное выражение (Pattern), которое может распознавать дробные числа со знаком.
        // В качестве разделителя используется точка
        Pattern pattern = Pattern.compile("-?\\d+\\.?\\d*");
        // Получаем объект matcher из Pattern, который непосредственно находит дробные числа в строке line
        Matcher matcher = pattern.matcher(line);

        // Инициализируем массив equation, который будет заполнен коэффициентами и свободным членом из
        // строки файла line
        double[] equation = new double[m];
        // Находим в цикле все дробные числа строки и записываем в equation
        for (int i = 0; i < m; i++) {
            // Находим следующее дробное число
            matcher.find();
            // Вырезаем из строки line дробное число, находящееся в строке между индексами
            // matcher.start() и matcher.end(), после чего приводим вырезанную строку к типу double
            equation[i] = Double.parseDouble(line.substring(matcher.start(), matcher.end()));
        }

        return equation;
    }

    // Метод prepareMatrix() ищет перестановку, для которой выполняется хотя бы условие отсутствия нулей на
    // главной диагонали, а еще лучше - ДУС. Если перестановки, для которой выполнется ДУС не найдено, но
    // существует перестановка, для которой условие отсутствие нулей на главной диагонали выполняется, то
    // будет возвращено false, то строки матрицы будут переставлены. Если нет и перестановки без нулей, то
    // в поле status будет записано сообщение о том, что данную систему нельзя решить итерационным методом.
    // В таком случае метод вернет также false.
    public boolean prepareMatrix() {
        // rowSums хранит в себе суммы всех коэффициентов для каждой строки исходной матрицы
        double[] rowSums = new double[n];
        for (int i = 0; i < n; i++) {
            rowSums[i] = 0.0;
            for (int j = 0; j < m - 1; j++) {
                rowSums[i] += matrix[i][j];
            }
        }

        // template - содержит переставку индексов строк
        int[] template = new int[n];
        for (int i = 0; i < n; i++) {
            template[i] = i;
        }

        // permute - рекурсивно находит все возможные перестановки строк и для каждой проверяет одновременно
        // оба условия. Если подходящая под оба условия перестановка найдена, то мы немеделнно возвращаемся из
        // рекурсии с возвращаемым значением true
        if (permute(template, rowSums, 0)) {
            // переставляем строки в соответствии с найденной перестановкой
            applyTemplate(template);

            System.out.println("Матрица с переставленными строками");
            System.out.println(format());
            return true;
        } else {
            // permute возвращает false если хотя бы одно условие не выполняется. Однако, если была найдена перестановка,
            // для которой главная диагонль не содержит нулей, первая такая найденная перестановка будет записана в
            // nonZeroTemplate
            if (nonZeroTemplate != null) {
                // применяем nonZeroTemplate над исходной матрицей matrix
                applyTemplate(nonZeroTemplate);
            } else {
                // выставляем статус
                status = "Данную систему решить итерационным методом нельзя";
            }

            System.out.println("Матрица на последней итерации перестановок");
            System.out.println(format());
            return false;
        }
    }

    // Метод applyTemplate() переставляет строки матрицы matrix в соответствии с перестанавкой template
    private void applyTemplate(int[] template) {
        // initailMatrix - содержит в себе исходную полную копию исходной матрицы. Из этой матрицы мы будем брать
        // строки для расстановки
        double[][] initialMatrix = new double[n][];
        for (int i = 0; i < n; i++) {
            initialMatrix[i] = new double[m];
            for (int j = 0; j < m; j++) {
                initialMatrix[i][j] = matrix[i][j];
            }
        }

        // сам процесс перестановки
        for (int i = 0; i < n; i++) {
            matrix[i] = initialMatrix[template[i]];
        }
    }

    // Метод permute() - рекурсивно ищет перестановки. Параметр k означает индекс элемента в permutation,
    // который будет подвергаться перестановкам на текущем этапе рекурсии.
    private boolean permute(int[] permutation, double[] rowSums, int k) {
        // условие выхода из рекурсии. Если k == n - 1, то мы получили очередную уникальную перестановку
        if (k == n - 1) {
            // проверяем условия при данной перестановке
            return isValidPermutation(permutation, rowSums);
        }

        // цикл, переставляющий каждый элемент с индексом большим или равным k на место с индексом k
        for (int i = k; i < n; i++) {
            // переставляем два элемента в массиве
            swap(permutation, i, k);

            // рекурсивный вызов permute с параметром k + 1. То есть, определив новое число на позицию k,
            // мы ищем все перестановки, для которых на позиции k это число.
            if (permute(permutation, rowSums, k + 1)) {
                // если была найдена перестановка, для которой выполняются оба условия, немедленно прекращаем
                // рекурсию. Так как из текущего этапа рекурсивного вызова возвращается true, то и все
                // вышележащие по стеку вызовов этапы будут немедленно прекращены.
                return true;
            }

            // переставляем возвращаем элементы на изначальные позиции
            swap(permutation, k, i);
        }

        // подходящая перестановка с элементов с k по n - 1 при фиксированных с 0 по k - 1 не была найдена
        return false;
    }

    // Метод swap() осуществляет переставку двух элементов массива array с индексами i и j
    private void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    // Метод isValidPermutation() проверяет оба условия для перестановки permutation
    private boolean isValidPermutation(int[] permutation, double[] rowSums) {
        // Если была найдена первая перестановка строк, для которой на главной диагонали нет нулей, то
        // запоминаем ее в nonZeroTempalate
        if (nonZeroTemplate == null && isNoZeroOnMainDiagonal(permutation)) {
            nonZeroTemplate = new int[n];
            for (int i = 0; i < n; i++) {
                nonZeroTemplate[i] = permutation[i];
            }
        }

        // вызываем функции проверки условий
        return isNoZeroOnMainDiagonal(permutation) && isDusCondition(permutation, rowSums);
    }

    // Метод isNoZeroOnMainDiagonal() проверяет условие отсутствия нулей на главной диагонали, для перестановки permutation
    private boolean isNoZeroOnMainDiagonal(int[] permutation) {
        for (int i = 0; i < n; i++) {
            if (isAEqualToB(matrix[permutation[i]][i], 0.0)) {
                return false;
            }
        }

        return true;
    }

    // Метод isDusCondition() проверяет достаточное условие сходимости
    private boolean isDusCondition(int[] permutation, double[] rowSums) {
        // sum - сумма модулей
        double sum;
        // notEqualFlag - true означает, что была найдена хотя бы одна строка, для которой ДУС выполняется строго,
        // false - нет ни одной строки, для которой ДУС бы выполнялся строго
        boolean notEqualFlag = false;
        for (int i = 0; i < n; i++) {
            sum = rowSums[permutation[i]] - matrix[permutation[i]][i];

            if (Math.abs(matrix[permutation[i]][i]) - sum > EPSILON) {
                // Если нашли переставноку, для которой ДУС выполняется строго (в пределах точности EPSILON), то
                // устанавливаем в notEqualFlag true
                notEqualFlag = true;
            } else if (!isAEqualToB(Math.abs(matrix[permutation[i]][i]), sum)) {
                // Если ДУС нарушется, немедленно завершаем выполнение функции и возращаем false
                return false;
            }
        }

        // Мы не нашли ни одной строки, для которой ДУС нарушался бы. Значит ответ хранится в переменной notEqualFlag.
        // Если была найдена хотя бы одна строка, для которой ДУС выполняется строго, она будет равна true.
        return notEqualFlag;
    }

    // метод solve() выполняет алгоритм поиска решений методом Гаусса-Зейделя
    public void solve(boolean dus) {
        // выделяем память под массив решений
        solutions = new double[n];
        // выделяем память под массив приближений текущей итерации
        currentIteration = new double[n];
        // берем в качестве изначального приближения массив нулей
        for (int j = 0; j < n; j++) {
            solutions[j] = 0.0;
        }

        if (dus) {
            // если ДУС выполняется, то мы уверены, что за конечное число итераций найдем решение,
            // поэтому просто выполняем в цикле итерации, пока не достигнем требуемой точности
            double maxDifference = -1.0;
            while (Math.abs(maxDifference) >= precision) {
                maxDifference = iteration();
            }
        } else {
            // k - хранит число пройденных итераций
            int k = 1;
            // oldDifference - максимальный модуль разницы с прошлой итерации, либо -1, если это первая итерация
            double oldDifference = -1.0;
            // maxDifference - максимальный модуль разницы для текущей итерации, либо -1, если мы еще не прошли
            // первую итерацию
            double maxDifference = -1.0;
            while (Math.abs(maxDifference) >= precision) {
                // если привешено число итераций, записываем сообщение в статус и прерываем цикл
                if (k > MAX_ITERATIONS_AMOUNT) {
                    status = "Метод расходится";
                    break;
                }

                // выполняем очередную итерацию
                maxDifference = iteration();

                // если это первая итерацию, то oldDifference == -1.0 и значит мы не можем судить о монотонности убывания
                if (isAEqualToB(oldDifference, -1.0)) {
                    oldDifference = maxDifference;
                } else if (maxDifference - oldDifference > EPSILON) {
                    // проверяем монотонность убывания моделей разностей, если заметили нарушение, то есть
                    // максимальный модуль разности за текущую итерацию больше, чем за предыдущую,
                    // то записываем статус и прерываем цикл
                    status = "Метод расходится. Немонотонное убывания модуля разностей обнаружено на итерации: " + k;
                    break;
                } else {
                    // все хорошо, нужно лишь обновить запись oldDifference перед следующей итерацией
                    oldDifference = maxDifference;
                }
                // увеличиваем порядковый номер следующей итерации
                k++;
            }
        }
    }

    // метод iteration() проводит очередную итерацию по методу Гаусcа-Зейделя. Возвращает модуль максимальной разности
    // решений между предыдущей и текущей итерациями
    private double iteration() {
        // maxDifference хранит модуль наибольшей разницы для данной итерации
        double maxDifference = -1.0;
        // проходимся по всем строчкам
        for (int i = 0; i < n; i++) {
            // findXi() реализует итерационную формулу Гаусса-Зейделя
            currentIteration[i] = findXi(i);

            // обновляем, если нужно maxDifference
            if (Math.abs(solutions[i] - currentIteration[i]) - maxDifference > EPSILON) {
                maxDifference = Math.abs(solutions[i] - currentIteration[i]);
            }
        }

        // После завершения итерации необходимо перенести найденные решение в solutions
        for (int i = 0; i < n; i++) {
            solutions[i] = currentIteration[i];
        }

        return maxDifference;
    }
    // метод findXi() находит очередное приближение решения Xi, используя итерационную формулу Гаусса-Зейделя
    private double findXi(int i) {
        // xi хранит результат выполнение функции. Инициализируем свободным членом
        double xi = matrix[i][m - 1];

        // проходимся по строке i
        for (int j = 0; j < n; j++) {
            // в случае совпадения индексов i и j пропускаем итерацию
            if (i == j) {
                continue;
            }

            // если индекс j > i, то нужно использовать решение с предыдущей итерации, т.е. solutions
            if (j > i) {
                xi += -1.0 * matrix[i][j] * solutions[j];
            } else  {
                // если же индес j < i, то нужно использовать решение с текущей итерации, т.е. currentIteration
                xi += -1.0 * matrix[i][j] * currentIteration[j];
            }
        }

        return xi / matrix[i][i];
    }

    // метод находит сумму модулей всех коэффициентов в строке с индексом rowIndex, кроме элемента с индексом i
    private double sumForRowI(int rowIndex, int i) {
        int sum = 0;
        for (int k = 0; k < n; k++) {
            if (k != i) {
                sum += Math.abs(matrix[rowIndex][k]);
            }
        }
        return sum;
    }

    // меняет две строки в matrix местами
    private void swapLines(int indexA, int indexB) {
        // Запоминаем строку с индексом indexA во временной переменной tmp
        double[] tmp = matrix[indexA];
        // В строку с индексом indexA записываем строку indexB
        matrix[indexA] = matrix[indexB];
        // В строку с индексом indexB записываем запомненную строку с indexA, содержащуюся в tmp
        matrix[indexB] = tmp;
    }

    // Представляет матрицу matrix в виде строки
    public String format() {
        String result = "";
        // В цикле форматируем каждую строку матрицы matrix, используя нашу функцию formatEquation, результат
        // на каждой итерации добавляем к result
        for (int i = 0; i < n; i++) {
            result += formatEquation(matrix[i]);
        }
        return result;
    }

    // Представляет строку equation в виде строки. Использует String.format() для форматированного вида (колонки +
    // экспоненциальный вид записи дробных чисел)
    private String formatEquation(double[] equation) {
        String result = "";
        // В цикле форматируем каждую элемент строки матрицы matrix, используя String.format() для
        // форматированного вывода, результат на каждой итерации добавляем к result
        for (int i = 0; i < m; i++) {
            result += String.format("%18.6e", equation[i]);
            // Если это последний элемент строки, добавляем разделитель строк.
            // Для обеспечения кроссплатформенности используем системную функцию System.lineSeparator(),
            // которая возвращает правильный разделить строк: \r\n для Windows, \n для UNIX систем.
            if (i == m - 1) {
                result += System.lineSeparator();
            }
        }
        return result;
    }

    // Возвращает строку, содержащую форматированный вывод массива решений solutions.
    public String getSolutions() {
        String result = "";
        // В цикле форматируем каждую элемент массива неизвестных solutions, используя String.format() для
        // форматированного вывода, результат на каждой итерации добавляем к result
        for (int i = 0; i < m - 1; i++) {
            result += String.format("%18.6e", solutions[i]);
        }
        return result;
    }

    // Возвращает информационное сообщение
    public String getStatus() {
        return status;
    }
}
