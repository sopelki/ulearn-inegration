using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

public interface ICourseApiClient
{
    Task<IReadOnlyList<Course>> GetCoursesAsync(CancellationToken ct = default);
    Task LoadCoursePracticesAsync(Course course, CancellationToken ct = default);
    Task<Practice> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct = default);
}