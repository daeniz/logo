/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myresprog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * @author dennisschmock
 */
public class Interpreter {

    private Point currentPoint;
    private Point oldPoint;
    private LineReader lr;
    private ArrayList<String[]> commands;
    private String[] command;
    private String[] commandToExecute;
    private int programCounter;
    private MainPanel mainPanel;
    private double angle;
    private HashMap vars = new HashMap();
    private ArrayList<Loop> loops;
    private HashMap procedures = new HashMap();
    private ArrayList<Procedure> runningProcedures = new ArrayList<>();

    public Interpreter(MainPanel panel) {
        mainPanel = panel;
        lr = new LineReader("Commands.txt");
        commands = lr.textFileToStringArrayList();
        angle = 0;
        oldPoint = new Point(400, 300);
        currentPoint = oldPoint;
        loops = new ArrayList<>();
        scanForProcedures();
        
    }

    public void performNextAction() {
        command = this.getCommands().get(getProgramCounter());
        makeCommandToExecute();
        substituteVars(commandToExecute);
        interpretCommand(commandToExecute);
        System.out.println(programCounter);
        programCounter++;
    }

    private void makeCommandToExecute() {
        commandToExecute = new String[2];
        commandToExecute[0] = command[0];
        commandToExecute[1] = "";

        for (int i = 1; i < command.length; i++) {
            if (i == 1) {
                commandToExecute[1] += command[i];
            } else {
                commandToExecute[1] += " " + command[i];
            }
        }
    }

    private void substituteVars(String[] command) {             //Substitute declared variables with values at time of execution
        System.out.println("Subvars running");
        if (!command[0].equals("let") && !command[0].equals("repeat")&& !command[0].equals("call")&& !command[0].equals("declare")) {
            String expString = "";
            String[] temp = command[1].split(",");
            for (int i = 0; i < temp.length; i++) {
                for (int k = 0; k < temp.length; k++) {
                    expString = temp[k];
                }
                System.out.println("Expr string: " + expString);
                Expression e = new ExpressionBuilder(expString).variables(vars.keySet()).build();
                e.setVariables(vars);
                commandToExecute[1] = commandToExecute[1].replace(temp[i], String.valueOf(e.evaluate()));
            }
        }
    }

    private void interpretCommand(String[] command) {
        String tempString = command[0].toLowerCase();
        switch (tempString) {
            case "fd":
                if (isNumeric(command[1])) {
                    currentPoint = FindPoint.findNewPoint(oldPoint, (int) Double.parseDouble(command[1]), angle);
                }
                mainPanel.drawLine(oldPoint, new Point(FindPoint.getInt(currentPoint.getX()), FindPoint.getInt(currentPoint.getY())));
                oldPoint = currentPoint;
                break;
            case "bk":
                currentPoint = FindPoint.findNewPoint(oldPoint, (int) Double.parseDouble(command[1]) * -1, angle);
                mainPanel.drawLine(oldPoint, new Point(FindPoint.getInt(currentPoint.getX()), FindPoint.getInt(currentPoint.getY())));
                oldPoint = currentPoint;
                break;
            case "rt":
                angle = angle + Double.parseDouble(command[1]);
                break;
            case "lt":
                angle = angle - Double.parseDouble(command[1]);
                break;
            case "let":
                let();
                break;
            case "repeat":
                repeatCommands(command);
                break;
            case "color":
                changeColor(command);
                break;
            case "mf":
                movePointerForward(command);
                break;
            case "mb":
                movePointerBack(command);
                break;
            case "declare":
                handleProcedure(command);
                break;
            case "call":
                callProcedure(command);
                break;
            case "moveto":
                moveTo(command);
                break;
            case "turnto":
                turnTo(command);
                break;
            default:
        }
    }

    public void let() {
        String expString = "";
        for (int i = 2; i < this.command.length; i++) {                 // Add everything after "let <var>"
            expString += this.command[i];                               // to expString
        }
        Expression e = new ExpressionBuilder(expString).variables(vars.keySet()).build();
        e.setVariables(vars);
        commandToExecute[1] = commandToExecute[1].split(" ")[0];          // Find name of variable
        vars.put(commandToExecute[1], e.evaluate());                    // Map name to value in map

        System.out.println("Expression added: " + commandToExecute[1] + " with the value " + e.evaluate());
    }

    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private void repeatCommands(String[] command) {
//        // mainPanel.timer.stop();
        if (command.length <= 1) {
            return;
        }
        if (command[1].equalsIgnoreCase("end")) {
            if (!loops.isEmpty()) {
                Loop intLoop = loops.get(loops.size() - 1);

                if (intLoop.getIterations() <= 0) {
                    loops.remove(intLoop);
                    System.out.println("Ended loop");
                    return;
                }
                System.out.println("Jumping to: " + intLoop.getStartIndex());
                programCounter = intLoop.getStartIndex();
                intLoop.setIterations(intLoop.getIterations() - 1);
            }
        }
        if (isNumeric(command[1])) {
            loops.add(new Loop("Loop" + programCounter, programCounter, 0, (int) Double.parseDouble(command[1]) - 1));
        }
    }

    public void scanForProcedures() {
        int tempInt = 0;
        int endProc;

        for (String[] command1 : commands) {
            if (command1.length <= 0) {
            } else if (command1[0].equalsIgnoreCase("declare") && command1.length <= 1) {
                System.out.println("Do nothing");

            } else if (command1[0].equalsIgnoreCase("declare") && !command1[1].equalsIgnoreCase("end")) {
                endProc = findEndProc(commands, tempInt);
                declareProcedure(command1, tempInt, endProc);
            }
            tempInt++;
        }

    }

    private int findEndProc(ArrayList<String[]> commands, int tempInt) {
        for (int i = tempInt; i < commands.size(); i++) {
            String tempCommand[] = commands.get(i);
            if (tempCommand[0].equalsIgnoreCase("declare") && tempCommand[1].equalsIgnoreCase("end")) {
                return i;
            }

        }
        return 0;

    }

    private void handleProcedure(String[] command) {
        if (command.length <= 1) {
            return;
        }
        if (command[1].equalsIgnoreCase("end")) {
            Procedure tempProc = runningProcedures.get(runningProcedures.size() - 1);
            programCounter = tempProc.getCallPoint();
            runningProcedures.remove(tempProc);
            return;
        }
        System.out.println(command[1]);
        Procedure tempProc = (Procedure) procedures.get(command[1]);
        System.out.println(tempProc);
        programCounter = tempProc.getProcEnd();

    }

    private void moveTo(String[] command) {
        if (command.length > 1) {
            System.out.println("Cmd 1 " + command[1]);
            String[] coordinates = command[1].split(",");
            if (coordinates.length > 1 && isNumeric(coordinates[0]) && isNumeric(coordinates[1])) {
                currentPoint.setX(Double.parseDouble(coordinates[0]));
                currentPoint.setY(Double.parseDouble(coordinates[1]));
                oldPoint = currentPoint;

            }
        }

    }

    private void turnTo(String[] command) {
        if (command.length > 1 && isNumeric(command[1])) {
            System.out.println("Lykkedes");
        }

    }

    private void declareProcedure(String[] command, int start, int end) {
        if (command.length <= 1) {
            return;
        }
        
        Procedure tempProc = new Procedure(command[1], start, end, command);
        procedures.put(command[1], tempProc);
        System.out.println("Name of proc:" + command[1]);
        if (command.length > 2) {
            for (int i = 2; i < command.length; i++) {
                tempProc.getLocalVars().put(command[i], 0);

            }

        }

    }

    private void callProcedure(String[] command) {
        System.out.println("Trying to call:" + command[1]);
        if (command.length <= 1) {
            System.out.println("Return, size is: " + command.length);
            return;
        }
        for (String command1 : command) {
            System.out.print(command1 + " ");
        }
        Procedure tempProc = (Procedure) procedures.get(command[1]);
        if (command.length > 2&&procedures.containsKey(command[1])) {

            for (int i = 2; i < command.length; i++) {
                String tempVar = "tempvar" + i;
                if (tempProc.getProcedureCommand().length==command.length){
                    tempVar = tempProc.getProcedureCommand()[i];
                }
                tempProc.getLocalVars().put(tempVar, command[i]);
            }
        }
            tempProc.setCallPoint(programCounter);
            programCounter = tempProc.getProcStart();
            runningProcedures.add(tempProc);

        
    }

    

    private void changeColor(String[] s) {
        for (String s1 : s) {
            System.out.print(s1 + " ");
        }
        int temp = 0;
        if (s.length > 1&&isNumeric(s[1])) {
            temp = Integer.parseInt(s[1]);
            if(temp>13){
                temp = temp%13;
            }
        }
        if (temp==0)mainPanel.setColor(Color.BLACK);
        if (temp==1)mainPanel.setColor(Color.BLUE);
        if (temp==2)mainPanel.setColor(Color.CYAN);
        if (temp==3)mainPanel.setColor(Color.DARK_GRAY);
        if (temp==4)mainPanel.setColor(Color.GRAY);
        if (temp==5)mainPanel.setColor(Color.GREEN);
        if (temp==6)mainPanel.setColor(Color.LIGHT_GRAY);
        if (temp==7)mainPanel.setColor(Color.MAGENTA);
        if (temp==8)mainPanel.setColor(Color.ORANGE);
        if (temp==9)mainPanel.setColor(Color.PINK);
        if (temp==10)mainPanel.setColor(Color.RED);
        if (temp==11)mainPanel.setColor(Color.WHITE);
        if (temp==12)mainPanel.setColor(Color.YELLOW);
       
        
        
        
            
        
       
        
    }

    private void movePointerForward(String[] command) {
        if (isNumeric(command[1])) {
            currentPoint = FindPoint.findNewPoint(oldPoint, Integer.parseInt(command[1]), angle);
        }
        oldPoint = currentPoint;
    }

    private void movePointerBack(String[] command) {
        if (isNumeric(command[1])) {
            currentPoint = FindPoint.findNewPoint(oldPoint, Integer.parseInt(command[1]) * -1, angle);
        }
        oldPoint = currentPoint;
    }

    /**
     * @return the currentPoint
     */
    public Point getCurrentPoint() {
        return currentPoint;
    }

    /**
     * @param currentPoint the currentPoint to set
     */
    public void setCurrentPoint(Point currentPoint) {
        this.currentPoint = currentPoint;
    }

    /**
     * @return the oldPoint
     */
    public Point getOldPoint() {
        return oldPoint;
    }

    /**
     * @param oldPoint the oldPoint to set
     */
    public void setOldPoint(Point oldPoint) {
        this.oldPoint = oldPoint;
    }

    /**
     * @return the commands
     */
    public ArrayList<String[]> getCommands() {
        return commands;
    }

    /**
     * @param aCommands the commands to set
     */
    public void setCommands(ArrayList<String[]> aCommands) {
        commands = aCommands;
    }

    /**
     * @return the programCounter
     */
    public int getProgramCounter() {
        return programCounter;
    }

    /**
     * @param aProgramCounter the programCounter to set
     */
    public void setProgramCounter(int aProgramCounter) {
        programCounter = aProgramCounter;
    }

    public void reset() {
        programCounter = 0;
        angle = 0;
        oldPoint = new Point(400, 300);
        currentPoint = oldPoint;
        loops = new ArrayList<>();

    }

}
