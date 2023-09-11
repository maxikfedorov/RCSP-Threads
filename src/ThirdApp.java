import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.*;

public class ThirdApp {
    // Константы для генерации файлов и управления очередью
    private static final int MIN_DELAY_MS = 100;
    private static final int MAX_DELAY_MS = 1000;
    private static final int MIN_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int QUEUE_CAPACITY = 5;

    public static void main(String[] args) throws InterruptedException {
        // Создание ExecutorService с фиксированным пулом потоков из двух потоков
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Создание очереди и компонентов системы
        Queue<File> queue = new LinkedList<>();
        Generator generator = new Generator(queue);
        Processor processor = new Processor(queue);

        // Запуск генератора и обработчика в отдельных потоках
        executor.submit(generator);
        executor.submit(processor);

        // Ожидание 10 секунд
        Thread.sleep(10000);

        // Остановка генератора и обработчика
        generator.stop();
        processor.stop();

        // Завершение работы ExecutorService
        executor.shutdown();
    }

    // Класс, который генерирует файлы и добавляет их в очередь
    private static class Generator implements Runnable {
        private final Queue<File> queue;
        private volatile boolean stopped;

        public Generator(Queue<File> queue) {
            this.queue = queue;
        }

        public void stop() {
            stopped = true;
        }

        @Override
        public void run() {
            Random random = new Random();
            while (!stopped) {
                try {
                    // Задержка для генерации файла
                    Thread.sleep(random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1) + MIN_DELAY_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Генерация типа и размера файла
                FileType type = FileType.values()[random.nextInt(FileType.values().length)];
                int size = random.nextInt(MAX_SIZE - MIN_SIZE + 1) + MIN_SIZE;
                File file = new File(type, size);
                synchronized (queue) {
                    // Ожидание, если очередь заполнена
                    while (queue.size() >= QUEUE_CAPACITY) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // Добавление файла в очередь и уведомление обработчика
                    queue.add(file);
                    System.out.println("Сгенерирован файл " + file);
                    queue.notifyAll();
                }
            }
        }
    }

    // Класс, который обрабатывает файлы из очереди
    private static class Processor implements Runnable {
        private final Queue<File> queue;
        private volatile boolean stopped;

        public Processor(Queue<File> queue) {
            this.queue = queue;
        }

        public void stop() {
            stopped = true;
        }

        @Override
        public void run() {
            while (!stopped) {
                File file;
                synchronized (queue) {
                    // Ожидание, если очередь пуста
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // Удаление файла из очереди и уведомление генератора
                    file = queue.remove();
                    queue.notifyAll();
                }
                try {
                    // Обработка файла
                    Thread.sleep(file.getSize() * 7);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Обработан файл " + file);
            }
        }
    }

    // Класс, который представляет файл
    private static class File {
        private final FileType type;
        private final int size;

        public File(FileType type, int size) {
            this.type = type;
            this.size = size;
        }

        public FileType getType() {
            return type;
        }

        public int getSize() {
            return size;
        }

        @Override
        public String toString() {
            return "File{" +
                    "type=" + type +
                    ", size=" + size +
                    '}';
        }
    }

    // Перечисление, которое определяет типы файлов
    private enum FileType {
        XML,
        JSON,
        XLS
    }
}
