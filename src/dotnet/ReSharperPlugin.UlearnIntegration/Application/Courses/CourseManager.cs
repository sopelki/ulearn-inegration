using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

namespace ReSharperPlugin.UlearnIntegration.Application.Courses;

public class CourseManager(ICourseApiClient courseApiClient)
{
    private readonly Dictionary<string, Course> coursesCache = [];
    private IReadOnlyList<Course> cachedCoursesList;
    
    public async Task<IReadOnlyList<Course>> GetCoursesAsync(CancellationToken ct = default)
    {
        if (cachedCoursesList != null)
            return cachedCoursesList;
        
        cachedCoursesList = await courseApiClient.GetCoursesAsync(ct);
        
        foreach (var course in cachedCoursesList)
            coursesCache[course.Id] = course;

        return cachedCoursesList;
    }
    
    public async Task<Course> LoadCoursePracticesAsync(string courseId, CancellationToken ct = default)
    {
        if (!coursesCache.TryGetValue(courseId, out var course))
        {
            await GetCoursesAsync(ct);
            course = coursesCache[courseId] ?? throw new KeyNotFoundException($"Курс с ID '{courseId}' не найден.");
        }

        if (course.IsLoaded)
            return course;
        
        await courseApiClient.LoadCoursePracticesAsync(course, ct);
        return course;
    }
    
    public async Task<Practice> OpenPracticeAsync(string courseId, string slideId, CancellationToken ct = default)
    {
        return await courseApiClient.GetSlideDetailsAsync(courseId, slideId, ct);
    }
}