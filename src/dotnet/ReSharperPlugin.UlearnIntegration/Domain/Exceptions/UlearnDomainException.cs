using System;

namespace ReSharperPlugin.UlearnIntegration.Domain.Exceptions;

// сетевая ошибка Ulearn
public class UlearnDomainException : Exception
{
    public UlearnDomainException(string message) : base(message) { }
    
    public UlearnDomainException(string message, Exception innerException) : base(message, innerException) { }
}