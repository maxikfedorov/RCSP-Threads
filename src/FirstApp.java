import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;

public class FirstApp {
    // Константы для генерации массива и управления задержкой
    private static final int ARRAY_SIZE = 10000;
    private static final int DELAY_MS = 1;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Генерация случайного массива
        int[] array = generateRandomArray(ARRAY_SIZE);

        // Поиск максимального элемента последовательно
        long start = System.nanoTime();
        int max = findMaxSequentially(array);
        long end = System.nanoTime();
        System.out.println("Последовательно: " + max + ", время: " + (end - start) + " нс");

        // Поиск максимального элемента с использованием многопоточности
        start = System.nanoTime();
        max = findMaxWithThreads(array);
        end = System.nanoTime();
        System.out.println("С использованием многопоточности: " + max + ", время: " + (end - start) + " нс");

        // Поиск максимального элемента с использованием ForkJoin
        start = System.nanoTime();
        max = findMaxWithForkJoin(array);
        end = System.nanoTime();
        System.out.println("С использованием ForkJoin: " + max + ", время: " + (end - start) + " нс");
    }

    // Метод для генерации случайного массива
    private static int[] generateRandomArray(int size) {
        int[] array = new int[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt();
        }
        return array;
    }

    // Метод для поиска максимального элемента последовательно
    private static int findMaxSequentially(int[] array) throws InterruptedException {
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            Thread.sleep(DELAY_MS);
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    // Метод для поиска максимального элемента с использованием многопоточности
    private static int findMaxWithThreads(int[] array) throws InterruptedException, ExecutionException {

        // Определение количества потоков, которые будут использоваться для обработки массива
        int threadsCount = Runtime.getRuntime().availableProcessors();

        // Создание ExecutorService с фиксированным количеством потоков
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        // Создание массива Future, который будет содержать результаты выполнения задач в потоках
        Future<Integer>[] futures = new Future[threadsCount];

        // Разбиение массива на сегменты и обработка каждого сегмента в отдельном потоке
        for (int i = 0; i < threadsCount; i++) {
            int startIndex = i * (array.length / threadsCount);
            int endIndex = (i == threadsCount - 1) ? array.length : (i + 1) * (array.length / threadsCount);
            // Выполнение задачи findMaxSequentially в отдельном потоке с помощью executor.submit
            futures[i] = executor.submit(() -> findMaxSequentially(Arrays.copyOfRange(array, startIndex, endIndex)));
        }

        // Получение результатов выполнения задач в потоках и нахождение максимального элемента
        int max = futures[0].get();
        for (int i = 1; i < threadsCount; i++) {
            // Задержка для симуляции работы потоков
            Thread.sleep(DELAY_MS);
            int result = futures[i].get();
            if (result > max) {
                max = result;
            }
        }

        // Завершение работы ExecutorService
        executor.shutdown();
        return max;
    }

    // Метод для поиска максимального элемента с использованием ForkJoin
    private static int findMaxWithForkJoin(int[] array) {
        // Создание ForkJoinPool
        ForkJoinPool pool = new ForkJoinPool();
        // Запуск выполнения задачи поиска максимального элемента в массиве
        return pool.invoke(new FindMaxTask(array, 0, array.length));
    }

    // Класс, который представляет задачу поиска максимального элемента с использованием ForkJoin
    private static class FindMaxTask extends RecursiveTask<Integer> {
        // Пороговое значение для определения, когда нужно перестать делить массив на сегменты и начать обрабатывать их последовательно
        private static final int THRESHOLD = 10;
        private final int[] array;
        private final int start;
        private final int end;

        public FindMaxTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            // Если размер сегмента меньше порогового значения, то обрабатываем его последовательно
            if (end - start <= THRESHOLD) {
                int max = array[start];
                for (int i = start + 1; i < end; i++) {
                    try {
                        Thread.sleep(DELAY_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (array[i] > max) {
                        max = array[i];
                    }
                }
                return max;
            } else {
                // Иначе делим сегмент на две части и обрабатываем их рекурсивно
                int middle = (start + end) / 2;
                FindMaxTask leftTask = new FindMaxTask(array, start, middle);
                FindMaxTask rightTask = new FindMaxTask(array, middle, end);
                leftTask.fork();
                int rightResult = rightTask.compute();
                int leftResult = leftTask.join();
                return Math.max(leftResult, rightResult);
            }
        }
    }

}
