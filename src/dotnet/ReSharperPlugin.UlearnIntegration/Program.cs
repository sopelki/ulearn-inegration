using System;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.Rider.Model;

namespace ReSharperPlugin.UlearnIntegration
{
    [SolutionComponent]
    public class UlearnAuthService
    {
        public UlearnAuthService(ISolution solution, Lifetime lifetime)
        {
            // Сгенерированную модель протокола
            var model = solution.GetProtocolSolution().GetUlearnModel();

            // Подписываемся на вызов из Kotlin на логин
            model.Login.Set((reqLifetime, request) => HandleLoginAsync(request));
        }

        private static Task<LoginResponse> HandleLoginAsync(LoginRequest request)
        {
            // ВРЕМЕННАЯ ЛОГИКА
            var ok = request.Login == "admin" && request.Password == "123";

            return Task.FromResult(ok
                ? new LoginResponse(true, "Успешный вход!")
                : new LoginResponse(false, "Неверный логин или пароль"));
        }
    }
}