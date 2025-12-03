import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
//Special thanks to https://lobehub.com/mcp/anima-labs-terminal-sessions-mcp for controll sequence documentation for cursor positioning and clearing console


public class HCI_Chatbot 
{

//idle animation control 
static volatile boolean IdleAnimationRunning = false;
static volatile boolean IdlePaused = false;

public static void main (String[] args) 
{
    Scanner input = new Scanner(System.in);


    clearScreen();
    System.out.print("\033[?25l"); //hide cursor to mask animation
    System.out.flush();

    //Display chatbot and start its dialouge
    ChatbotAvatar();
    ChatbotDialouge();

    String[] demographics = GetUserDemographicInfo(input); //get user demogrphic info

    //Start Idle blinking while asking Likert questions
    IdleAnimationRunning = true;
    Thread idleThread = new Thread(() -> startIdleBlink());
    idleThread.start();

    //Start Likert Scale Questionnaire and save user results directly to output file
    saveAnswers(LikertScale(input), demographics); 

    //stop idle after done
    IdleAnimationRunning = false;
    try { idleThread.join(100); } catch (InterruptedException e) { /* ignore */ }

    System.out.print("\033[?25h"); // show cursor again at program end
    System.out.flush();

}



//################################### HELPER METHODS ########################


//Default Chatbot avatar
public static void ChatbotAvatar()
{
    String[] ChatBotAvatar = 
    {
        "#############",
        "#   #    #  #",
        "#     *     #",
        "#     -     #",
        "#############"
    };

    printAvatarOnly(ChatBotAvatar);

} //END


//cursor controll - CHANGE AT YOUR OWN RISK
public static void moveCursor(int row, int col)
{
    System.out.print("\033[" + row + ";" + col + "H");
    System.out.flush();
}

//save cursor position
public static void saveCursor()
{
    System.out.print("\033[s");
    System.out.flush();
}

//restore cursor position
public static void restoreCursor()
{
    System.out.print("\033[u");     //restore position
    System.out.print("\033[?25l");  //force-hide cursor
    System.out.flush();
}


//Print only avatar lines 1-5 without moving cursor (KEEPS avatar thread from stealing cursor for questions)
public static void printAvatarOnly(String[] avatar)
{
    int row = 1;
    for(String line: avatar)
    {
        moveCursor(row,1);
        System.out.print(line);
        row++;
    }
    System.out.flush();
}

//overwrite only the mouth (line 4)
public static void overwriteMouthLine(String mouthLine)
{
    moveCursor(4,1);
    System.out.print(mouthLine);
    System.out.flush();
}


//Idle blink animation
public static void startIdleBlink()
{
    String[] open =
    {
        "#############",
        "#   #    #  #",
        "#     *     #",
        "#     -     #",
        "#############"
    };

    String[] closed =
    {
        "#############",
        "#   _    _  #",
        "#     *     #",
        "#     -     #",
        "#############"
    };

    //display avatar once at start
    printAvatarOnly(open);

    while (IdleAnimationRunning)
    {
        if (IdlePaused)
        {
            try { Thread.sleep(100); } catch (InterruptedException e){}
            continue;
        }

        // blink closed
        saveCursor();
        printAvatarOnly(closed);
        restoreCursor();
        try { Thread.sleep(250); } catch (InterruptedException e){}

        // blink open
        saveCursor();
        printAvatarOnly(open);
        restoreCursor();
        try { Thread.sleep(3000); } catch (InterruptedException e){}
    }

    // restore open avatar at the end
    saveCursor();
    printAvatarOnly(open);
    restoreCursor();
}


//Speaking animation that replaces only the mouth line while printing dialogue below
public static void chatbotSpeakingAnimation(String dialogue)
{
    String[] ChatBotAvatar = 
    {
        "#############",
        "#   #    #  #",
        "#     *     #",
        "#     -     #",
        "#############"
    };

    String[] ChatBotAvatarMouthOpen = 
    {
        "#############",
        "#   #    #  #",
        "#     *     #",
        "#     #     #",
        "#############"
    };

    //pause idle animation
    IdlePaused = true;

    //print avatar base
    printAvatarOnly(ChatBotAvatar);

    //dialogue row: avatar is 5 lines, row 7 for dialogue - NEWLINES IN QUESTIONS WILL BREAK
    int dialogueRow = 7;
    moveCursor(dialogueRow,1);
    System.out.print(dialogue + "\n");
    System.out.flush();

    //speaking animation
    for(int i=0;i<6;i++)
    {
        overwriteMouthLine(ChatBotAvatarMouthOpen[3]);
        try{ Thread.sleep(200); } catch(InterruptedException e){}
        overwriteMouthLine(ChatBotAvatar[3]);
        try{ Thread.sleep(200); } catch(InterruptedException e){}
    }

    // leave avatar mouth closed and cursor below dialogue
    overwriteMouthLine(ChatBotAvatar[3]);
    moveCursor(dialogueRow+1,1);
    System.out.flush();

    IdlePaused = false;
}


//Chatbot dialogue
public static void ChatbotDialouge()
{
    String[] ChatbotDialouge = 
    {
        "Hello, I am ChatBot and I will be administering your questionnaire today",
        "All questions are to be answered based on how you feel on a scale of 1-5",
        "Please answer honestly", //e printing on end
        "Before we begin could you please answer the following questions.\n"
    };

    System.out.println(); //extra spacing

    for (int i = 0; i < 4; i++)
    {
        chatbotSpeakingAnimation(ChatbotDialouge[i]);
    }

    System.out.println();
}


//Likert scale
public static int[] LikertScale(Scanner input) 
{
    int[] userAnswers = new int[12];
    String LikerScaleCriteria = "\nStrongly Disagree   Disagree    Neutral     Agree    Strongly Agree";
    String LikertScaleNumbers = "       (1)              (2)        (3)        (4)           (5)\n";
    String[] LikertQuestions = likertQuestions();

    for(int i=0;i<LikertQuestions.length;i++)
    {
        //clear only below avatar for each question
        moveCursor(7,1);
        System.out.print("\033[J"); //clear from cursor down
        System.out.flush();

        //print questions
        System.out.println("\n\n\t\t\t" + LikertQuestions[i] + "\n"); //extra /n to fix line clear issue i know its not clean
        System.out.println(LikerScaleCriteria);
        System.out.println(LikertScaleNumbers);

        int answer=0;
        while(true)
        {
            System.out.print("\nEnter your response (1-5): ");
            answer=input.nextInt();
            if(answer >= 1 && answer <= 5) break;
            else System.out.println("Invalid input. Please enter a number between 1 and 5.");
        }
        userAnswers[i]=answer;
    }

    return userAnswers;
}


// Likert questions
public static String[] likertQuestions()
{
    return new String[]
    {
        "I enjoyed this CAPTCHA Quiz",
        "I would take this quiz again",
        "This CAPTCHA quiz was easy",
        "Taking this quiz caused me little frustration",
        "Taking this quiz didn’t make me angry",
        "I understood what the questions on the quiz were asking",
        "I felt little stress taking this quiz",
        "The questions asked felt necessary to ensure a user is a human",
        "I felt satisfied when I answered the questions correctly",
        "I have had extensive experience with CAPTCHAs in the past",
        "This was more enjoyable than my past experiences with CAPTCHAs",
        "I would recommend this quiz to friends"
    };
}


//user demographic info
public static String[] GetUserDemographicInfo(Scanner input) 
{
    System.out.print("Please enter your age: ");
    String age = input.nextLine(); 

    System.out.print("Please enter your gender: ");
    String gender = input.nextLine();

    return new String[]{age, gender};
}

//user demographic info
public static String[] GetOpenQuestions(Scanner input) 
{
    System.out.print("Did you enjoy that test and why? ");
    String q1 = input.nextLine(); 
    System.out.print("Did you feel any frustration when completing the questions? How are you feeling? ");
    String q2 = input.nextLine();
    System.out.print("Was there a moment in time where you struggled the most? ");
    String q3 = input.nextLine();    
    System.out.print("How can we do better next time? ");
    String q4 = input.nextLine();  
    System.out.print("Do you think you would take that test again? ");
    String q5 = input.nextLine();

    return new String[]{q1, q2, q3, q4, q5};
}


//Clear screen
public static void clearScreen()
{
    System.out.print("\033[H\033[2J");
    System.out.flush();
}


//save answers to csv
public static void saveAnswers(int[] userAnswers, String[] demographics)
{
    String[] LikertQuestions = likertQuestions();
    int participantNumber = getNextParticipantNumber(); 
    String fileName = "Participant" + participantNumber + ".csv";

    IdlePaused = true;

    try(FileWriter writer = new FileWriter(fileName))
    {
        writer.write("Questions,Answers\n");
        for(int i=0; i<LikertQuestions.length; i++)
        {
            writer.write("\"" + LikertQuestions[i] + "\"," + userAnswers[i] + "\n");
        }
        Scanner input = new Scanner(System.in);
        String[] genQuestions = GetOpenQuestions(input);
        writer.write("\nGeneral Questions,\n");
        for (int j = 0; j < genQuestions.length; j++) {
            writer.write(genQuestions[j] + "\n");
        }

        writer.write("\nDemographic Information,\n");
        writer.write("Age," + demographics[0] + "\n");
        writer.write("Gender," + demographics[1] + "\n");


        //Clear Likert section for thank you
        moveCursor(7,1);
        System.out.print("\033[J");
        System.out.flush();

        //speaking animation for thank you
        chatbotSpeakingAnimation("Results successfully saved to " + fileName + "!\nThank you for your participation");
        try {Thread.sleep(1000);} catch(InterruptedException e){}

    }
    catch(IOException e){System.out.println("Error writing to file: "+e.getMessage());}
    finally{IdlePaused=false;}
}


//participant number management and tracking
public static int getNextParticipantNumber()
{
    int participantNumber = 1;
    File counterFile = new File("participant_counter.txt");

    try
    {
        if(counterFile.exists())
        {
            Scanner reader = new Scanner(counterFile);
            if(reader.hasNextInt()) participantNumber = reader.nextInt()+1;
            reader.close();
        }
        FileWriter writer = new FileWriter(counterFile);
        writer.write(String.valueOf(participantNumber));
        writer.close();
    }
    catch(IOException e){System.out.println("Error managing participant counter: "+e.getMessage());}

    return participantNumber;
}

} // END OF FILE
