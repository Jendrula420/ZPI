package com.sourcey.materiallogindemo;

public class CodesStore
{
    String code;
    String indexNumber;
    String date;
    String time;
    String deviceID;
    String latitude;
    String longitude;
    String groupID          = "";
    String thisClassesNumber= "";
    String thisClassesDate  = "";
    String thisClassesTime  = "";
    String lecturerID       = "";
    String courseID         = "";
    String evenWeek         = "";
    String classesWeekday   = "";
    String classesPlace     = "";
    String classesRoom      = "";
    String classesStartTime = "";
    String classesEndTime   = "";
    String lecturer         = "";
    String courseName       = "";
    int late                = -1;
    int distance            = -1;
    boolean strictVerify    = true;
    //TODO remove
    String info            = "";

    CodesStore(String[] split)
    {
        this.code           = split[0];
        this.indexNumber    = split[1];
        this.date           = split[2];
        this.time           = split[3];
        this.deviceID       = split[4];
        this.latitude       = split[5];
        this.longitude      = split[6];
    }

    CodesStore(String code, String indexNumber, String dateTime, String deviceID,  String localization)
    {
        this.code           = code;
        this.indexNumber    = indexNumber;
        String[] temp   = dateTime.split("[;]");
        this.date           = temp[0];
        this.time           = temp[1];
        this.deviceID       = deviceID;
        temp            = localization.split("[;]");
        this.latitude       = temp[0];
        this.longitude      = temp[1];
    }

    public void setClasses(String[] split)
    {
        thisClassesNumber    = split[0];
        thisClassesDate  = split[1];
        thisClassesTime  = split[2];
    }

    public void verify()
    {
        if (late == -1)
            dateVerify();
        if (distance == -1)
            distanceVerify();
    }

    private void dateVerify()
    {
        int result = 4;
        if(date.equals(thisClassesDate))
        {
            int dt = timeDifference(time, thisClassesTime);
            if(dt>0)
                result = strictVerify?timeStrictVerify(dt):timeVerify(dt);
            else
                result = 0;
        }
        late = result;
    }

    private int timeDifference(String t1, String t2)
    {
        String[] a = t1.split("[:]");
        int m1 = Integer.parseInt(a[0])*60+Integer.parseInt(a[1]);
        a = t2.split("[:]");
        int m2 = Integer.parseInt(a[0])*60+Integer.parseInt(a[1]);
        return m1-m2;
    }

    private int timeVerify(int dt)
    {
        dt = (dt + 14) / 15;
        return dt>4?4:dt;
    }

    private int timeStrictVerify(int dt)
    {
        return dt<=15?1:4;
    }

    public String lateToString()
    {
        if(late==-1)
            verify();
        return late==0?"obecność":late==1?"spóźnienie":late==2?"średnie spóźnienie":late==3?"duże spóźnienie":late==4?"nieobecność":"Error";
    }

    private void distanceVerify()
    {
        //TODO latitude, longitude verify;
    }

    @Override
    public String toString()
    {
        if(late==-1)
            verify();
        String result = "Kurs: "            +courseName;
        result += "\nProwadzący: "          +lecturer;
        result += "\nMiejsce: "             +classesPlace+" "+classesRoom;
        result += "\nCzas zajęć: "          +classesWeekday+" "+(!evenWeek.equals("")?evenWeek+" ":"")
                + classesStartTime+"-"+classesEndTime;
        result += "\nZajęcia numer: "       +thisClassesNumber;
        result += "\nZajęcia rozpoczęte: "  +thisClassesDate+" "+thisClassesTime;
        result += "\nObecność odnotowana: " +date+" "+time;
        result += "\nRodzaj obecnosci: "    +lateToString();
        if(!info.equals(""))
            result += "\nInfo: "                +info;
        return result;
    }
}
