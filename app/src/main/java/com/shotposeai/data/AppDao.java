package com.shotposeai.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AppDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) long insertUser(UserEntity user);
    @Query("SELECT * FROM users WHERE lower(email)=lower(:email) LIMIT 1") UserEntity findUserByEmail(String email);
    @Update int updateUser(UserEntity user);

    @Insert long insertStudent(StudentEntity student);
    @Update int updateStudent(StudentEntity student);
    @Delete int deleteStudent(StudentEntity student);
    @Query("DELETE FROM analysis_results WHERE trainingSessionId IN (SELECT id FROM training_sessions WHERE studentId=:studentId)") void deleteStudentAnalysis(long studentId);
    @Query("DELETE FROM training_sessions WHERE studentId=:studentId") void deleteStudentTraining(long studentId);
    @Query("DELETE FROM class_students WHERE studentId=:studentId") void removeStudentFromClasses(long studentId);
    @Query("DELETE FROM student_achievements WHERE studentId=:studentId") void deleteStudentAchievements(long studentId);
    @Query("SELECT * FROM students ORDER BY name") List<StudentEntity> getStudents();
    @Query("SELECT * FROM students WHERE id=:id LIMIT 1") StudentEntity getStudent(long id);
    @Query("SELECT COUNT(*) FROM students") int studentCount();

    @Insert long insertClass(ClassRoomEntity classroom);
    @Update int updateClass(ClassRoomEntity classroom);
    @Delete int deleteClass(ClassRoomEntity classroom);
    @Query("DELETE FROM class_students WHERE classId=:classId") void removeAllStudentsFromClass(long classId);
    @Query("SELECT * FROM classrooms ORDER BY createdAt DESC") List<ClassRoomEntity> getClasses();
    @Query("SELECT * FROM classrooms WHERE id=:id LIMIT 1") ClassRoomEntity getClassById(long id);
    @Query("SELECT COUNT(*) FROM classrooms") int classCount();
    @Query("SELECT COUNT(*) FROM class_students WHERE classId=:classId") int classStudentCount(long classId);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void addStudentToClass(ClassStudentEntity crossRef);
    @Query("DELETE FROM class_students WHERE classId=:classId") void clearClassStudents(long classId);

    @Insert long insertTraining(TrainingSessionEntity session);
    @Insert long insertAnalysis(AnalysisResultEntity result);
    @Query("SELECT COUNT(*) FROM training_sessions") int trainingCount();
    @Query("SELECT COUNT(*) FROM training_sessions WHERE score >= 75") int passedCount();
    @Query("SELECT COALESCE(AVG(score),0) FROM training_sessions") double averageScore();
    @Query("SELECT COALESCE(MAX(score),0) FROM training_sessions WHERE studentId=:studentId") int bestScore(long studentId);
    @Query("SELECT COUNT(*) FROM training_sessions WHERE studentId=:studentId") int studentTrainingCount(long studentId);
    @Query("SELECT s.id sessionId, s.studentId studentId, st.name studentName, s.movementName movementName, s.score score, s.level level, s.date date, " +
           "COALESCE(a.correctPoints,'') correctPoints, COALESCE(a.improvementPoints,'') improvementPoints, COALESCE(a.coachNote,'') coachNote " +
           "FROM training_sessions s JOIN students st ON st.id=s.studentId LEFT JOIN analysis_results a ON a.trainingSessionId=s.id ORDER BY s.date DESC")
    List<ReportRow> getReports();
    @Query("SELECT s.id sessionId, s.studentId studentId, st.name studentName, s.movementName movementName, s.score score, s.level level, s.date date, " +
           "COALESCE(a.correctPoints,'') correctPoints, COALESCE(a.improvementPoints,'') improvementPoints, COALESCE(a.coachNote,'') coachNote " +
           "FROM training_sessions s JOIN students st ON st.id=s.studentId LEFT JOIN analysis_results a ON a.trainingSessionId=s.id WHERE s.id=:id LIMIT 1")
    ReportRow getReport(long id);

    @Query("SELECT movementName, MAX(score) bestScore FROM training_sessions WHERE studentId=:studentId AND status='COMPLETED' GROUP BY movementName")
    List<MovementBest> getMovementBest(long studentId);
    @Query("SELECT COUNT(DISTINCT studentId) FROM training_sessions WHERE movementName=:movement AND score>=:threshold AND status='COMPLETED' AND studentId IN (SELECT id FROM students)")
    int completedStudentCount(String movement, int threshold);
    @Query("SELECT st.* FROM students st JOIN class_students cs ON cs.studentId=st.id WHERE cs.classId=:classId ORDER BY st.name")
    List<StudentEntity> getClassStudents(long classId);
    @Query("SELECT COALESCE(AVG(score),0) FROM training_sessions WHERE status='COMPLETED' AND studentId IN (SELECT studentId FROM class_students WHERE classId=:classId)")
    double classAverageScore(long classId);
    @Query("SELECT COUNT(DISTINCT movementName) FROM training_sessions WHERE status='COMPLETED' AND score>=85 AND studentId IN (SELECT studentId FROM class_students WHERE classId=:classId)")
    int classCompletedMovementCount(long classId);
    @Query("SELECT s.id sessionId, s.studentId studentId, st.name studentName, s.movementName movementName, s.score score, s.level level, s.date date, " +
           "COALESCE(a.correctPoints,'') correctPoints, COALESCE(a.improvementPoints,'') improvementPoints, COALESCE(a.coachNote,'') coachNote " +
           "FROM training_sessions s JOIN students st ON st.id=s.studentId LEFT JOIN analysis_results a ON a.trainingSessionId=s.id WHERE s.studentId=:studentId ORDER BY s.date DESC")
    List<ReportRow> getStudentReports(long studentId);
    @Query("SELECT * FROM users WHERE id=:id LIMIT 1") UserEntity getUser(long id);
    @Query("SELECT * FROM achievements WHERE id=:id LIMIT 1") AchievementEntity getAchievement(long id);
    @Query("SELECT COUNT(*) FROM student_achievements WHERE studentId=:studentId AND achievementId=:achievementId")
    int isAchievementUnlocked(long studentId, long achievementId);
    @Query("SELECT * FROM training_sessions WHERE id=:id LIMIT 1") TrainingSessionEntity getTraining(long id);

    @Insert long insertAchievement(AchievementEntity achievement);
    @Query("SELECT * FROM achievements WHERE name=:name LIMIT 1") AchievementEntity getAchievementByName(String name);
    @Query("SELECT * FROM achievements ORDER BY requirementValue") List<AchievementEntity> getAchievements();
    @Insert(onConflict = OnConflictStrategy.IGNORE) long unlock(StudentAchievementEntity unlocked);
    @Query("SELECT a.* FROM achievements a JOIN student_achievements sa ON sa.achievementId=a.id WHERE sa.studentId=:studentId")
    List<AchievementEntity> getUnlocked(long studentId);
}
