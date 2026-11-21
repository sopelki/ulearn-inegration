namespace ReSharperPlugin.UlearnIntegration.Application.Practices;


// TODO: проверить надо ли оно вообще, мб с версией везде юзать + перенести в domain + чёта ещё
public class PracticeContext(string courseId, string slideId)
{
    public string CourseId { get; set; } = courseId;
    public string SlideId { get; set; } =  slideId;
}