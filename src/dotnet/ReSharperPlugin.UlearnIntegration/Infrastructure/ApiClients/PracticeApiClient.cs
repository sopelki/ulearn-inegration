// using System;
// using System.Collections.Generic;
// using System.Linq;
// using System.Threading;
// using System.Threading.Tasks;
// using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
// using ReSharperPlugin.UlearnIntegration.Application.Practices;
// using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
// using UlearnApi.Client;
//
// namespace ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;
//
// public class PracticeApiClient(ApiClient apiClient): IPracticeApiClient
// {
//     public async Task<SlideDto> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct)
//     {
//         var response = await RetryPolicy.ExecuteAsync(() =>
//             apiClient.Slides[courseId][Guid.Parse(slideId)].GetAsync(cancellationToken: ct)
//         );
//
//         if (response == null)
//             throw new InvalidOperationException($"Не удалось получить данные практики '{slideId}' для курса '{courseId}'.");
//
//         // var htmlContent = string.Empty;
//         // if (response.Blocks != null)
//         // {
//         //     var htmlBlocks = response.Blocks
//         //         .Where(b => b.GetType().GetProperty("$type")?.GetValue(b)?.ToString() == "html" || 
//         //                     b.ToString()?.Contains("Html") == true)
//         //         .Select(b => b.GetType().GetProperty("Content")?.GetValue(b)?.ToString())
//         //         .Where(content => !string.IsNullOrEmpty(content));
//         //
//         //     htmlContent = string.Join("\n", htmlBlocks);
//         // }
//         
//         var htmlContent = string.Empty;
//         if (response.Blocks != null)
//         {
//             var htmlBlocks = response.Blocks
//                 .Where(b => string.Equals(GetAdditionalString(b, "$type"), "html", StringComparison.OrdinalIgnoreCase))
//                 .Select(b => GetAdditionalString(b, "content"))
//                 .Where(content => !string.IsNullOrEmpty(content));
//
//             htmlContent = string.Join("\n", htmlBlocks);
//         }
//         
//         var hints = new List<string>();
//         if (response.Blocks != null)
//         {
//             foreach (var block in response.Blocks)
//             {
//                 var hintsProperty = block.GetType().GetProperty("RenderedHints");
//                 if (hintsProperty?.GetValue(block) is IEnumerable<string> blockHints)
//                 {
//                     hints.AddRange(blockHints);
//                 }
//             }
//         }
//         
//         return new SlideDto(response.Id.ToString(), response.Title ?? "Без названия", htmlContent, hints);
//     }
//     
//     private static string? GetAdditionalString(object block, string key)
//     {
//         if (block is not Microsoft.Kiota.Abstractions.Serialization.IAdditionalDataHolder holder ||
//             holder.AdditionalData == null ||
//             !holder.AdditionalData.TryGetValue(key, out var rawValue) ||
//             rawValue == null)
//         {
//             return null;
//         }
//
//         // В Kiota значение в AdditionalData может лежать как string или как UntypedString
//         if (rawValue is string str)
//             return str;
//
//         var valueProp = rawValue.GetType().GetProperty("Value");
//         if (valueProp != null)
//             return valueProp.GetValue(rawValue)?.ToString();
//
//         return rawValue.ToString();
//     }
// }


// using System;
// using System.Collections.Generic;
// using System.Linq;
// using System.Text.Json;
// using System.Threading;
// using System.Threading.Tasks;
// using Microsoft.Kiota.Abstractions.Serialization;
// using Microsoft.Kiota.Serialization.Json;
// using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
// using ReSharperPlugin.UlearnIntegration.Application.Practices;
// using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
// using UlearnApi.Client;
//
// namespace ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;
//
// public class PracticeApiClient(ApiClient apiClient) : IPracticeApiClient
// {
//     public async Task<SlideDto> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct)
//     {
//         if (!Guid.TryParse(slideId, out var slideGuid))
//             throw new InvalidOperationException($"Идентификатор слайда '{slideId}' в файле .ulearn не является GUID.");
//
//         var response = await RetryPolicy.ExecuteAsync(() =>
//             apiClient.Slides[courseId][slideGuid].GetAsync(cancellationToken: ct)
//         );
//
//         if (response == null)
//             throw new InvalidOperationException($"Не удалось получить данные практики '{slideId}' для курса '{courseId}'.");
//
//         var htmlParts = new List<string>();
//         var hints = new List<string>();
//         var blockTypes = new List<string>();
//
//         if (response.Blocks != null)
//         {
//             foreach (var block in response.Blocks)
//             {
//                 if (block is not IParsable parsable)
//                     continue;
//
//                 blockTypes.Add(block.GetType().Name);
//
//                 // Сериализуем блок обратно в JSON средствами самой Kiota и читаем поля по именам
//                 // из ответа сервера ($type, content, renderedHints). Так код не зависит ни от имён
//                 // сгенерированных классов блоков, ни от того, лежат ли поля в свойствах или в обёртке.
//                 using var json = ToJson(parsable);
//                 var root = json.RootElement;
//                 if (root.ValueKind != JsonValueKind.Object)
//                     continue;
//
//                 var type = GetString(root, "$type");
//                 var content = GetString(root, "content");
//
//                 // Если тип в JSON не сохранился, считаем HTML-блоком любой блок с полем content.
//                 var isHtml = type != null
//                     ? string.Equals(type, "html", StringComparison.OrdinalIgnoreCase)
//                     : !string.IsNullOrEmpty(content);
//
//                 if (isHtml && !string.IsNullOrEmpty(content))
//                     htmlParts.Add(content);
//
//                 if (Find(root, "renderedHints") is { ValueKind: JsonValueKind.Array } hintsArray)
//                 {
//                     foreach (var hint in hintsArray.EnumerateArray())
//                     {
//                         if (hint.ValueKind == JsonValueKind.String)
//                             hints.Add(hint.GetString());
//                     }
//                 }
//             }
//         }
//
//         // ВРЕМЕННО, для отладки: если текст не нашёлся, вкладка покажет, какие блоки пришли.
//         // Когда всё заработает, лучше вернуть пустой HTML, чтобы слайды без текста не считались ошибкой.
//         if (htmlParts.Count == 0)
//             throw new InvalidOperationException(
//                 $"В слайде не найден HTML-блок (блоков: {blockTypes.Count}; типы: {string.Join(", ", blockTypes.Take(5))}).");
//
//         return new SlideDto(
//             response.Id.ToString(),
//             response.Title ?? "Без названия",
//             string.Join("\n", htmlParts),
//             hints);
//     }
//
//     private static JsonDocument ToJson(IParsable value)
//     {
//         using var writer = new JsonSerializationWriter();
//         writer.WriteObjectValue<IParsable>(null, value);
//         using var stream = writer.GetSerializedContent();
//         return JsonDocument.Parse(stream);
//     }
//
//     // Поиск свойства без учёта регистра.
//     private static JsonElement? Find(JsonElement obj, string name)
//     {
//         foreach (var property in obj.EnumerateObject())
//         {
//             if (string.Equals(property.Name, name, StringComparison.OrdinalIgnoreCase))
//                 return property.Value;
//         }
//
//         return null;
//     }
//
//     private static string GetString(JsonElement obj, string name)
//         => Find(obj, name) is { ValueKind: JsonValueKind.String } element ? element.GetString() : null;
// }


using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Kiota.Abstractions;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Application.Practices;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
using UlearnApi.Client;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;

public class PracticeApiClient(ApiClient apiClient, IRequestAdapter requestAdapter) : IPracticeApiClient
{
    public async Task<SlideDto> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct)
    {
        if (!Guid.TryParse(slideId, out var slideGuid))
            throw new InvalidOperationException($"Идентификатор слайда '{slideId}' в файле .ulearn не является GUID.");
        
        var requestInfo = apiClient.Slides[courseId][slideGuid].ToGetRequestInformation();

        using var stream = await RetryPolicy.ExecuteAsync(() =>
            requestAdapter.SendPrimitiveAsync<Stream>(requestInfo, cancellationToken: ct)
        );

        if (stream == null)
            throw new InvalidOperationException($"Не удалось получить данные практики '{slideId}' для курса '{courseId}'.");

        using var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct);
        var root = doc.RootElement;

        var htmlParts = new List<string>();
        var hints = new List<string>();
        var blockTypes = new List<string>();

        if (Find(root, "blocks") is { ValueKind: JsonValueKind.Array } blocks)
        {
            foreach (var block in blocks.EnumerateArray())
            {
                if (block.ValueKind != JsonValueKind.Object)
                    continue;

                var type = GetString(block, "$type");
                blockTypes.Add(type ?? "?");

                var content = GetString(block, "content");
                if (string.Equals(type, "html", StringComparison.OrdinalIgnoreCase) && !string.IsNullOrEmpty(content))
                    htmlParts.Add(content);

                if (Find(block, "renderedHints") is { ValueKind: JsonValueKind.Array } hintsArray)
                {
                    foreach (var hint in hintsArray.EnumerateArray())
                    {
                        if (hint.ValueKind == JsonValueKind.String)
                            hints.Add(hint.GetString());
                    }
                }
            }
        }
        
        return new SlideDto(
            slideId,
            GetString(root, "title") ?? "Без названия",
            string.Join("\n", htmlParts),
            hints);
    }
    
    private static JsonElement? Find(JsonElement obj, string name)
    {
        if (obj.ValueKind != JsonValueKind.Object)
            return null;

        foreach (var property in obj.EnumerateObject())
        {
            if (string.Equals(property.Name, name, StringComparison.OrdinalIgnoreCase))
                return property.Value;
        }

        return null;
    }

    private static string GetString(JsonElement obj, string name)
        => Find(obj, name) is { ValueKind: JsonValueKind.String } element ? element.GetString() : null;
}
