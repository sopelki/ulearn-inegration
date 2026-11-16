using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Domain.Models;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
using UlearnApi.Client;

namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public class CourseApiClient(ApiClient apiClient): ICourseApiClient
{
    public async Task<IReadOnlyList<Course>> GetCoursesAsync(CancellationToken ct = default)
    {
        var response = await RetryPolicy.ExecuteAsync(() =>
            apiClient.Courses.GetAsync(cancellationToken: ct)
        );

        if (response?.Courses == null)
            return Array.Empty<Course>();

        return response.Courses
            .Select(c => new Course(c.Id, c.Title, c.ApiUrl))
            .ToList();
    }

    public async Task LoadCoursePracticesAsync(Course course, CancellationToken ct = default)
    {
        var response = await RetryPolicy.ExecuteAsync(() =>
            apiClient.Courses[course.Id].GetAsync(cancellationToken: ct)
        );

        if (response?.Units == null)
        {
            course.SetUnits(Array.Empty<Unit>());
            return;
        }

        var unitsWithPractices = response.Units
            .Select(u =>
            {
                var practices = u.Slides?
                    .Where(s => !(s.Hide ?? false) && (s.RequiresReview ?? false))
                    .Select(s => new PracticeShortInfo(s.Id.ToString(), s.Title, s.MaxScore ?? 0))
                    .ToList() ?? new List<PracticeShortInfo>();

                return new Unit(u.Id.ToString(), u.Title, practices);
            })
            .Where(u => u.Practices.Count > 0)
            .ToList();

        course.SetUnits(unitsWithPractices);
    }

    public async Task<Practice> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct = default)
    {
        var response = await RetryPolicy.ExecuteAsync(() =>
            apiClient.Slides[courseId][Guid.Parse(slideId)].GetAsync(cancellationToken: ct)
        );

        if (response == null)
            throw new InvalidOperationException($"Не удалось получить данные практики '{slideId}' для курса '{courseId}'.");

        var htmlContent = string.Empty;
        if (response.Blocks != null)
        {
            var htmlBlocks = response.Blocks
                .Where(b => b.GetType().GetProperty("$type")?.GetValue(b)?.ToString() == "html" || 
                            b.ToString()?.Contains("Html") == true)
                .Select(b => b.GetType().GetProperty("Content")?.GetValue(b)?.ToString())
                .Where(content => !string.IsNullOrEmpty(content));

            htmlContent = string.Join("\n", htmlBlocks);
        }
        
        var hints = new List<string>();
        if (response.Blocks != null)
        {
            foreach (var block in response.Blocks)
            {
                var hintsProperty = block.GetType().GetProperty("RenderedHints");
                if (hintsProperty?.GetValue(block) is IEnumerable<string> blockHints)
                {
                    hints.AddRange(blockHints);
                }
            }
        }

        return new Practice(
            response.Id.ToString(),
            response.Title ?? "Без названия",
            htmlContent,
            hints
        );
    }
}