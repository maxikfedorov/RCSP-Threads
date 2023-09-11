import java.util.Scanner;
import java.util.concurrent.*;

public class SecondApp {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Создание Scanner для чтения ввода пользователя
        try (Scanner scanner = new Scanner(System.in)) {
            // Создание ExecutorService с одним потоком
            ExecutorService executor = Executors.newSingleThreadExecutor();

            // Бесконечный цикл для запроса чисел у пользователя
            while (true) {
                System.out.print("Введите число: ");
                int number = scanner.nextInt();

                // Создание Future для результата выполнения задачи в потоке ExecutorService
                Future<Integer> future = executor.submit(() -> {
                    // Генерация случайной задержки от 1 до 5 секунд
                    int delay = ThreadLocalRandom.current().nextInt(1, 6);
                    Thread.sleep(delay * 1000);
                    // Возведение числа в квадрат и возврат результата
                    return number * number;
                });

                // Ожидание завершения задачи и вывод результата
                while (!future.isDone()) {
                    System.out.println("Обработка запроса...");
                    Thread.sleep(500);
                }
                System.out.println("Результат: " + future.get());

                // Игнорирование дополнительных строк ввода пользователя
                if (scanner.hasNextLine()) {
                    scanner.nextLine();
                }
            }
        }
    }
}
