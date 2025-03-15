# TCP Soket Proxy Sunucusu

## Genel Bakış

Bu Java ile yazılmış TCP soket proxy sunucusu, istemciler ile hedef sunucular arasında veri aktarımı yapan bir ara katman yazılımıdır.

## Kodun Ne İşe Yaradığı

Bu kod, temel olarak bir TCP proxy veya "köprü" olarak çalışır. Şu işlemleri gerçekleştirir:

1. Belirtilen bir portta (varsayılan olarak 3131) dinleme yapar
2. Gelen bağlantıları kabul eder
3. Her bağlantı için belirtilen hedef sunucuya (varsayılan olarak 11.11.11.11:3306 - MySQL sunucusu) bağlanır
4. İki yönlü veri aktarımı sağlar:
   - İstemciden hedef sunucuya veri aktarır
   - Hedef sunucudan istemciye veri aktarır

## Teknik Özellikler

- Java NIO kanallarını kullanarak verimli veri aktarımı sağlar
- Eşzamanlı bağlantıları thread havuzu ile yönetir
- TCP_NODELAY özelliği ile Nagle algoritmasını devre dışı bırakarak gecikmeyi azaltır
- Ayarlanabilir tampon boyutları ve soket parametreleri sunar
- Bağlantılar kapandığında tüm kaynakları düzgün şekilde temizler

## Kullanım

Program şu şekilde başlatılır:

```bash
# Java dosyalarını derleyin
javac -d out org/exclover/Server.java

# Sunucuyu çalıştırın
java -cp out org.exclover.Server
```

Varsayılan olarak, program:
- 3131 portunda dinleme yapar
- Bağlantıları 11.11.11.11:3306 adresine yönlendirir (MySQL sunucusu)

## Kod Yapısı

Kod iki ana sınıftan oluşur:

1. **Server Sınıfı**:
   - Belirtilen portta dinleme yapar
   - Gelen bağlantıları kabul eder
   - Her bağlantı için bir SocketHandler oluşturur

2. **SocketHandler Sınıfı**:
   - İstemci ile hedef sunucu arasında veri aktarımını yönetir
   - NIO kanalları kullanarak verimli veri transferi sağlar
   - Bağlantıların düzgün şekilde kapatılmasını sağlar

## Gereksinimler

- Java 8 veya üstü
