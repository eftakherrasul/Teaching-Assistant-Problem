
import java.util.concurrent.Semaphore;
import java.util.Random;
public class TA 
{
    public static void main(String[] args) 
    {
        int numberofStudents = 5;
        // Create semaphores.
        SignalSemaphore wakeup = new SignalSemaphore();
        Semaphore chairs = new Semaphore(3);
        Semaphore available = new Semaphore(1);
        Random studentWait = new Random();
        for (int i = 0; i < numberofStudents; i++)
        {
            Thread student = new Thread(new Student(studentWait.nextInt(20), wakeup, chairs, available, i+1));
            student.start();
        }
        Thread ta = new Thread(new TeachingAssistant(wakeup, chairs, available));
        ta.start();
    }
}
class SignalSemaphore {
    private boolean signal = false;
    public synchronized void take() 
{
        this.signal = true;
        this.notify();
    }
    public synchronized void release() throws InterruptedException{
        while(!this.signal) wait();
        this.signal = false;
    }
}
class Student implements Runnable
{       
    private int programTime;
    private int studentNum;
    private SignalSemaphore wakeup;
    private Semaphore chairs;
    private Semaphore available;
    private Thread t;
    public Student(int program, SignalSemaphore w, Semaphore c, Semaphore a, int num)
    {
        programTime = program;    
        wakeup = w;
        chairs = c;
        available = a;
        studentNum = num;
        t = Thread.currentThread();
    }
    public void run()
    {
        while(true)
        {
            try
            {
               System.out.println("Student " + studentNum + " has started programming for " + programTime + " seconds.");
               t.sleep(programTime * 1000);
               System.out.println("Student " + studentNum + " is checking to see if TA is available.");
               if (available.tryAcquire())
               {
                   try
                   {
                       wakeup.take();
                       System.out.println("Student " + studentNum + " has woke up the TA.");
                       System.out.println("Student " + studentNum + " has started working with the TA.");
                       t.sleep(5000);
                       System.out.println("Student " + studentNum + " has stopped working with the TA.");
                   }
                   catch (InterruptedException e)
                   {
                       continue;
                   }
                   finally
                   {
                       available.release();
                   }
               }
               else
               {
                   System.out.println("Student " + studentNum + " could not see the TA.  Checking for available chairs.");
                   if (chairs.tryAcquire())
                   {
                       try
                       {
                           System.out.println("Student " + studentNum + " is sitting outside the office.  "
                                   + "He is #" + ((3 - chairs.availablePermits())) + " in line.");
                           available.acquire();
                           System.out.println("Student " + studentNum + " has started working with the TA.");
                           t.sleep(5000);
                           System.out.println("Student " + studentNum + " has stopped working with the TA.");
                           available.release();
                       }
                       catch (InterruptedException e)
                       {
                           continue;
                       }
                   }
                   else
                   {
                       System.out.println("Student " + studentNum + " could not see the TA and all chairs were taken.  Back to programming!");
                   }
               }
            }
            catch (InterruptedException e)
            {
                break;
            }
        }
    }
}
class TeachingAssistant implements Runnable
{
    private SignalSemaphore wakeup;
    private Semaphore chairs;
    private Semaphore available;
    private Thread t;
    public TeachingAssistant(SignalSemaphore w, Semaphore c, Semaphore a)
    {
        t = Thread.currentThread();
        wakeup = w;
        chairs = c;
        available = a;
    }
    public void run()
    {
        while (true)
        {
            try
            {
                System.out.println("No students left.  The TA is going to nap.");
                wakeup.release();
                System.out.println("The TA was awoke by a student.");
                t.sleep(5000);
                if (chairs.availablePermits() != 3)
                {
                    do
                    {
                        t.sleep(5000);
                        chairs.release();
                    }
                    while (chairs.availablePermits() != 3);                   
                }
            }
            catch (InterruptedException e)
            {
                continue;
            }
        }
    }
}