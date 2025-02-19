import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class q2 {
    // parameters
    private static int k;
    private static int q;

    // data structures
    private static int questionsAsked;
    private static int gradStudentsArrived;
    private static final Random random = new Random();
    private static final Lock lock = new ReentrantLock();
    private static final Condition wakeProfCondition = lock.newCondition();
    private static final Condition wakeGradStudentCondition = lock.newCondition();
    private static boolean taSignaled = false;
    private static boolean gradStudentSignaled = false;
    private static boolean profSignaled = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: q1.java k q");
            return;
        }

        k = Integer.parseInt(args[0]);
        q = Integer.parseInt(args[1]);

        try {
            // Start the Professor
            Professor professor = new Professor();
            professor.start();

            // Start the TAs
            TeachingAssistant[] teachingAssistants = new TeachingAssistant[k];
            for (int i = 0; i < k; i++) {
                teachingAssistants[i] = new TeachingAssistant(i);
                teachingAssistants[i].start();
            }

            // Start the GradStudents
            GradStudent[] gradStudents = new GradStudent[5];
            for (int i = 0; i < 5; i++) {
                gradStudents[i] = new GradStudent(i);
                gradStudents[i].start();
            }

            // Wait for GradStudents to finish
            for (GradStudent gradStudent : gradStudents) {
                gradStudent.join();
            }
            System.out.println("All grad students have been woken");

            // Terminate all threads
            for (TeachingAssistant teachingAssistant : teachingAssistants) {
                teachingAssistant.thread.interrupt();
            }
            professor.thread.interrupt();

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

    static class Professor implements Runnable {
        private Thread thread;

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        public void run() {
            lock.lock();
            try {
                while (true) {
                    while (!taSignaled && !gradStudentSignaled) {
                        wakeProfCondition.await();
                    }

                    System.out.println("P wakes");

                    if (taSignaled) {
                        System.out.println("A group of TAs starts to be seen by P");
                        Thread.sleep(500);
                        if (!gradStudentSignaled) {
                            questionsAsked = questionsAsked - 3;
                            System.out.println("A group of TAs questions have been answered");
                            taSignaled = false;
                            System.out.println("P goes to sleep");
                        } else {
                            System.out.println("A grad student interrupts a TA session");
                            gradStudentCall();
                        }
                    }

                    else {
                        gradStudentCall();
                    }
                }
            } catch (InterruptedException e) {

            } finally {
                lock.unlock();
            }
        }

        private void gradStudentCall() {
            System.out.println("P wakes their grad students");
            profSignaled = true;
            wakeGradStudentCondition.signalAll();
            gradStudentSignaled = false;
        }
    }

    static class TeachingAssistant implements Runnable {
        private Thread thread;
        private int name;

        public TeachingAssistant(int name) {
            this.name = name;
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        public void run() {
            try {
                while (true) {
                    int odds = random.nextInt(100);
                    if (odds < q) {
                        askQuestion();
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
            }
        }

        private void askQuestion() {
            lock.lock();
            try{
                questionsAsked = questionsAsked + 1;
                System.out.println("TA " + name + " comes up with a question");
                // Wake professor if group of 3 TAs
                if (questionsAsked >= 3) {
                    taSignaled = true;
                    wakeProfCondition.signal();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    static class GradStudent implements Runnable {
        private Thread thread;
        private int name;

        public GradStudent(int name) {
            this.name = name;
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        public void join() throws InterruptedException {
            thread.join();
        }

        public void run() {
            try {
                int arrivalTime = random.nextInt(10, 61) * 1000;
                Thread.sleep(arrivalTime);

                // After awakening, go to lab
                gradStudentsArrived++;
                System.out.println("Grad student " + name + " arrived");

                if (gradStudentsArrived == 5) {
                    awakenProf();
                }

                // Go to sleep until awoken by professor
                while (!profSignaled) {
                    lock.lock();
                    try {
                        while (!profSignaled) {
                            wakeGradStudentCondition.await();
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void awakenProf() {
            lock.lock();
            try {
                gradStudentSignaled = true;
                wakeProfCondition.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}
