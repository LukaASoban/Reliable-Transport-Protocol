/*
	Class for containing information on the student data (consider this the database)
*/
public class Student {

	public String first_name;
	public String last_name;
	public int quality_points;
	public int gpa_hours;
	public float gpa;

	public Student(String fname, String lname, int qpoints, int hours) {
		this.first_name = fname;
		this.last_name = lname;
		this.quality_points = qpoints;
		this.gpa_hours = hours;
		this.gpa = (float)qpoints/hours;
	}

}