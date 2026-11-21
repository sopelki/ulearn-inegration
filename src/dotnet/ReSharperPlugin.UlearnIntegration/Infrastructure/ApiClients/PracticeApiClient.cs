using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
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

        var html = new StringBuilder();
        var hints = new List<string>();
        // ReSharper disable once CollectionNeverQueried.Local
        var blockTypes = new List<string>();

        if (Find(root, "blocks") is { ValueKind: JsonValueKind.Array } blocks)
        {
            foreach (var block in blocks.EnumerateArray())
            {
                if (block.ValueKind != JsonValueKind.Object)
                    continue;

                var type = GetString(block, "$type");
                blockTypes.Add(type ?? "?");

                switch (type?.ToLowerInvariant())
                {
                    case "html":
                        AppendHtml(html, GetString(block, "content"));
                        break;

                    case "imagegallery":
                        AppendImages(html, GetStringArray(block, "imageUrls"));
                        break;

                    case "code":
                        AppendCode(html, GetString(block, "code"));
                        break;
                }

                hints.AddRange(GetStringArray(block, "renderedHints"));
            }
        }

        // Если ни один из известных блоков не найден, вместо ошибки показываем заметку с типами блоков.
        // if (html.Length == 0)
        // {
        //     var types = WebUtility.HtmlEncode(string.Join(", ", blockTypes.Distinct().Take(5)));
        //     html.AppendLine($"<p><i>В слайде нет блоков, которые можно показать (типы блоков: {types}).</i></p>");
        // }

        return new SlideDto(
            slideId,
            GetString(root, "title") ?? "Без названия",
            html.ToString(),
            hints);
    }

    private static void AppendHtml(StringBuilder html, string content)
    {
        if (!string.IsNullOrEmpty(content))
            html.AppendLine(content);
    }

    private static void AppendImages(StringBuilder html, IEnumerable<string> urls)
    {
        foreach (var url in urls)
        {
            var src = Uri.TryCreate(url, UriKind.Absolute, out var uri) ? uri.AbsoluteUri : url;
            html.AppendLine($"<p><img src=\"{WebUtility.HtmlEncode(src)}\"></p>");
        }
    }

    private static void AppendCode(StringBuilder html, string code)
    {
        if (!string.IsNullOrWhiteSpace(code))
            html.AppendLine($"<pre><code>{WebUtility.HtmlEncode(code)}</code></pre>");
    }

    // Поиск свойства без учёта регистра.
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

    private static List<string> GetStringArray(JsonElement obj, string name)
    {
        var result = new List<string>();

        if (Find(obj, name) is { ValueKind: JsonValueKind.Array } array)
        {
            foreach (var item in array.EnumerateArray())
            {
                if (item.ValueKind == JsonValueKind.String)
                    result.Add(item.GetString());
            }
        }

        return result;
    }
}
