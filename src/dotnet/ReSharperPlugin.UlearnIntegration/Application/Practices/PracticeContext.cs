namespace ReSharperPlugin.UlearnIntegration.Application.Practices;

public class PracticeContext(string courseId, string slideId)
{
    public string CourseId { get; set; }
    public string SlideId { get; set; }
}