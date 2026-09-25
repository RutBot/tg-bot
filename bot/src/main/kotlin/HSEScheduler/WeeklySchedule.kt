package HSEScheduler

import сurriculum.schedule.dsl.schedule
import сurriculum.schedule.model.LessonSchedule

class WeeklySchedule {
    fun mondaySchedule(): LessonSchedule = schedule("2023-10-23") {
            lesson {
                classroom("301")
                subject("Высшая математика")
                teacher("Иван Иванович Иванов")
                time("09:00 - 10:30")
                group("Группа 101")
            }

            lesson {
                classroom("405")
                subject("Олимпиадная физика")
                teacher("Петр Петрович Петров")
                time("10:40 - 12:10")
                group("Группа Физиков") {
                    students("Алексей", "Мария", "Дмитрий")
                }
            }

            lesson {
                classroom("301")
                subject("Линейная алгебра")
                teacher("Иван Иванович Иванов")
                time("12:20 - 13:50")
                group("Группа 101")
            }

            lesson {
                classroom("202")
                subject("Переписывание контрольной по информатике")
                teacher("Сидоров Сидор Сидорович")
                time("14:00 - 15:30")
                group {
                    students("Иван", "Петр", "Анна")
                }
            }
        }

    fun errorExample(): LessonSchedule = schedule("2023-10-23") {
        lesson {
            classroom("101")
            subject("Математика")
            teacher("Иванов")
            time("09:00 - 10:30")
            group("101")
        }

        lesson {
            classroom("101") // Та же аудитория
            subject("Физика")
            teacher("Петров")
            time("10:00 - 11:30") // Пересечение по времени (10:00 < 10:30)
            group("102")
        }
    }
}
