# Tá»•ng Quan Luá»“ng Cháº¡y

TÃ i liá»‡u nÃ y giáº£i thÃ­ch luá»“ng cháº¡y chÃ­nh cá»§a project theo thá»© tá»±:

- controller
- service Ä‘iá»u phá»‘i
- async worker
- tracker
- snapshot tráº£ vá» cho client

Má»¥c tiÃªu lÃ  giÃºp báº¡n nhÃ¬n há»‡ thá»‘ng nhÆ° má»™t cÃ¢u chuyá»‡n liá»n máº¡ch, thay vÃ¬ chá»‰ tháº¥y nhiá»u class rá»i ráº¡c.

## Bá»©c tranh lá»›n

Khi client gá»i:

```http
GET /jobs/start?count=10&baseUrl=https://picsum.photos/300/300
```

thÃ¬ há»‡ thá»‘ng khÃ´ng táº£i Ä‘á»“ng bá»™ ngay trÃªn thread HTTP.

Thay vÃ o Ä‘Ã³, nÃ³ lÃ m nhÆ° sau:

1. Controller nháº­n request.
2. Service táº¡o má»™t `job` má»›i.
3. Service sinh ra nhiá»u `task` con.
4. Má»—i task Ä‘Æ°á»£c Ä‘áº©y sang worker async.
5. Worker táº£i áº£nh vÃ  ghi file.
6. Tracker cáº­p nháº­t tráº¡ng thÃ¡i cá»§a tá»«ng task vÃ  cáº£ job.
7. Client gá»i endpoint status Ä‘á»ƒ xem snapshot hiá»‡n táº¡i.

## 1. Controller lÃ m gÃ¬?

File chÃ­nh:

- `DownloadController`

Vai trÃ² cá»§a controller lÃ :

- nháº­n HTTP request
- tÃ¡ch input tá»« query params
- gá»i sang service
- tráº£ response JSON vá» cho client

Controller **khÃ´ng nÃªn** lÃ  nÆ¡i chá»©a logic táº£i áº£nh tháº­t sá»±.

VÃ­ dá»¥ endpoint:

- `GET /jobs/start`
- `GET /jobs/{jobId}`
- `GET /jobs/{jobId}/tasks`

### Ã nghÄ©a tá»«ng endpoint

#### `GET /jobs/start`

DÃ¹ng Ä‘á»ƒ báº¯t Ä‘áº§u má»™t job má»›i.

NÃ³ tráº£ vá» ráº¥t nhanh vá»›i:

- `jobId`
- `count`
- `baseUrl`
- `message`

NÃ³ **khÃ´ng chá» táº£i xong**.

ÄÃ¢y lÃ  Ä‘iá»ƒm ráº¥t quan trá»ng trong thiáº¿t káº¿ async.

#### `GET /jobs/{jobId}`

Tráº£ vá» snapshot tá»•ng quan cá»§a job:

- cÃ²n bao nhiÃªu task Ä‘ang queue
- bao nhiÃªu task Ä‘ang cháº¡y
- Ä‘Ã£ xong bao nhiÃªu
- thÃ nh cÃ´ng bao nhiÃªu
- tháº¥t báº¡i bao nhiÃªu
- throughput hiá»‡n táº¡i ra sao

#### `GET /jobs/{jobId}/tasks`

Tráº£ vá» chi tiáº¿t tá»«ng task:

- URL nÃ o Ä‘ang cháº¡y
- task nÃ o tháº¥t báº¡i
- lá»—i gÃ¬
- thread nÃ o xá»­ lÃ½ task Ä‘Ã³
- máº¥t bao nhiÃªu mili giÃ¢y

## 2. Service Ä‘iá»u phá»‘i lÃ m gÃ¬?

File chÃ­nh:

- `DownloadJobService`

ÄÃ¢y lÃ  lá»›p orchestration, tá»©c lÃ  lá»›p Ä‘iá»u phá»‘i use case.

NÃ³ khÃ´ng trá»±c tiáº¿p táº£i áº£nh tá»«ng byte, mÃ  chá»‹u trÃ¡ch nhiá»‡m tá»• chá»©c cÃ´ng viá»‡c.

Khi `startJob(count, baseUrl)` Ä‘Æ°á»£c gá»i, service lÃ m 3 viá»‡c:

1. Táº¡o má»™t job má»›i trong tracker.
2. Sinh ra cÃ¡c task con tá»« `count` vÃ  `baseUrl`.
3. Gá»i worker async Ä‘á»ƒ thá»±c thi tá»«ng task.

VÃ­ dá»¥ náº¿u:

- `count = 3`
- `baseUrl = https://picsum.photos/300/300`

thÃ¬ service sáº½ sinh ra 3 URL:

- `https://picsum.photos/300/300?random=0`
- `https://picsum.photos/300/300?random=1`
- `https://picsum.photos/300/300?random=2`

Má»—i URL tÆ°Æ¡ng á»©ng vá»›i má»™t task.

## 3. Async worker lÃ m gÃ¬?

File chÃ­nh:

- `ImageDownloaderService`

ÄÃ¢y lÃ  nÆ¡i cÃ´ng viá»‡c tháº­t sá»± Ä‘Æ°á»£c thá»±c hiá»‡n.

Worker lÃ m cÃ¡c viá»‡c sau:

1. ÄÃ¡nh dáº¥u task tá»« `QUEUED` sang `RUNNING`.
2. Gá»i HTTP tá»›i URL cáº§n táº£i.
3. Kiá»ƒm tra `Content-Type` cÃ³ Ä‘Ãºng lÃ  áº£nh hay khÃ´ng.
4. Ghi file xuá»‘ng thÆ° má»¥c `downloads`.
5. Táº¡o `DownloadResult`.
6. BÃ¡o káº¿t quáº£ vá» tracker.

### VÃ¬ sao gá»i lÃ  async worker?

VÃ¬ method nÃ y Ä‘Æ°á»£c Ä‘Ã¡nh dáº¥u:

```java
@Async("imageTaskExecutor")
```

Äiá»u Ä‘Ã³ cÃ³ nghÄ©a lÃ  method sáº½ cháº¡y trÃªn thread pool, khÃ´ng cháº¡y trÃªn thread HTTP request ban Ä‘áº§u.

NÃ³i Ä‘Æ¡n giáº£n:

- request web chá»‰ cÃ³ nhiá»‡m vá»¥ khá»Ÿi Ä‘á»™ng cÃ´ng viá»‡c
- worker async má»›i lÃ  nÆ¡i tháº­t sá»± Ä‘i táº£i áº£nh

## 4. Thread pool tham gia á»Ÿ Ä‘Ã¢u?

File chÃ­nh:

- `AsyncConfig`

á»ž Ä‘Ã¢y ta táº¡o bean `imageTaskExecutor` báº±ng `ThreadPoolTaskExecutor`.

Thread pool cÃ³ nhiá»‡m vá»¥:

- giá»¯ sáºµn má»™t nhÃ³m worker thread
- task nÃ o Ä‘áº¿n thÃ¬ phÃ¢n cho worker ráº£nh
- náº¿u chÆ°a tá»›i lÆ°á»£t thÃ¬ task náº±m trong queue

CÃ¡c thÃ´ng sá»‘ nhÆ°:

- `corePoolSize`
- `maxPoolSize`
- `queueCapacity`

áº£nh hÆ°á»Ÿng trá»±c tiáº¿p Ä‘áº¿n cÃ¡ch task Ä‘Æ°á»£c xá»­ lÃ½.

### VÃ¬ sao bÃ i nÃ y há»£p vá»›i thread pool?

VÃ¬ Ä‘Ã¢y lÃ  bÃ i toÃ¡n I/O-bound.

Má»—i task táº£i áº£nh thÆ°á»ng tá»‘n nhiá»u thá»i gian chá»:

- chá» máº¡ng
- chá» server
- chá» response
- chá» ghi file

Khi task A Ä‘ang chá» máº¡ng, thread pool váº«n cÃ³ thá»ƒ cho task B hoáº·c C cháº¡y trÃªn thread khÃ¡c.

## 5. Tracker lÃ m gÃ¬?

File chÃ­nh:

- `DownloadJobTracker`

Tracker lÃ  nÆ¡i giá»¯ runtime state trong memory.

NÃ³ biáº¿t:

- hiá»‡n cÃ³ nhá»¯ng job nÃ o
- má»—i job cÃ³ nhá»¯ng task nÃ o
- task nÃ o Ä‘ang `QUEUED`
- task nÃ o Ä‘ang `RUNNING`
- task nÃ o `SUCCESS`
- task nÃ o `FAILED`

### VÃ¬ sao cáº§n tracker?

Náº¿u khÃ´ng cÃ³ tracker, há»‡ thá»‘ng chá»‰ biáº¿t:

- task Ä‘Ã£ cháº¡y
- log cÃ³ in ra console

NhÆ°ng sáº½ khÃ´ng tráº£ lá»i Ä‘Æ°á»£c cÃ¡c cÃ¢u há»i nhÆ°:

- job `1001` Ä‘ang tá»›i Ä‘Ã¢u rá»“i?
- task nÃ o fail?
- task nÃ o Ä‘ang cháº¡y?
- Ä‘Ã£ táº£i Ä‘Æ°á»£c bao nhiÃªu byte?
- throughput hiá»‡n táº¡i lÃ  bao nhiÃªu?

Tracker chÃ­nh lÃ  â€œsá»• theo dÃµi tiáº¿n Ä‘á»™â€ cá»§a há»‡ thá»‘ng.

### VÃ¬ sao tracker dÃ¹ng `synchronized`?

VÃ¬ nhiá»u worker thread cÃ³ thá»ƒ cÃ¹ng lÃºc cáº­p nháº­t cÃ¹ng má»™t job.

VÃ­ dá»¥:

- thread 1 vá»«a xong task 5
- thread 2 vá»«a xong task 6
- thread 3 vá»«a chuyá»ƒn task 7 sang `RUNNING`

Náº¿u khÃ´ng Ä‘á»“ng bá»™ truy cáº­p state, sá»‘ liá»‡u cÃ³ thá»ƒ bá»‹ lá»‡ch.

## 6. Snapshot lÃ  gÃ¬?

Snapshot lÃ  dá»¯ liá»‡u Ä‘Ã£ Ä‘Æ°á»£c tracker tá»•ng há»£p láº¡i Ä‘á»ƒ tráº£ cho client.

Hiá»‡n táº¡i cÃ³ 2 loáº¡i snapshot chÃ­nh:

- `DownloadJobSnapshot`
- `DownloadTaskSnapshot`

### `DownloadJobSnapshot`

ÄÃ¢y lÃ  bá»©c tranh lá»›n cá»§a cáº£ job.

NÃ³ cho biáº¿t:

- tá»•ng task
- queued
- running
- completed
- okCount
- failCount
- tá»•ng byte Ä‘Ã£ táº£i
- wall time của job (được đóng băng khi job hoàn tất)
- thá»i gian trung bÃ¬nh má»—i task
- throughput hiá»‡n táº¡i
- finished hay chÆ°a

### `DownloadTaskSnapshot`

ÄÃ¢y lÃ  áº£nh chá»¥p cá»§a tá»«ng task riÃªng láº».

NÃ³ cho biáº¿t:

- index cá»§a task
- URL cá»¥ thá»ƒ
- tráº¡ng thÃ¡i hiá»‡n táº¡i
- sá»‘ byte
- sá»‘ mili giÃ¢y
- content type
- lá»—i náº¿u cÃ³
- thread xá»­ lÃ½

## 7. Luá»“ng hoÃ n chá»‰nh báº±ng lá»i

HÃ£y Ä‘á»c cáº£ há»‡ thá»‘ng theo cÃ¢u chuyá»‡n sau:

1. Client gá»i `GET /jobs/start`.
2. `DownloadController` nháº­n request.
3. `DownloadJobService` táº¡o job má»›i.
4. `DownloadJobService` táº¡o nhiá»u task tá»« `count` vÃ  `baseUrl`.
5. Má»—i task Ä‘Æ°á»£c Ä‘Äƒng kÃ½ vÃ o `DownloadJobTracker` vá»›i tráº¡ng thÃ¡i ban Ä‘áº§u lÃ  `QUEUED`.
6. `DownloadJobService` gá»i `ImageDownloaderService.downloadImage(...)` cho tá»«ng task.
7. VÃ¬ cÃ³ `@Async`, má»—i task Ä‘Æ°á»£c Ä‘áº©y sang thread pool `imageTaskExecutor`.
8. Worker báº¯t Ä‘áº§u cháº¡y, Ä‘Ã¡nh dáº¥u task thÃ nh `RUNNING`.
9. Worker gá»i HTTP, nháº­n dá»¯ liá»‡u, kiá»ƒm tra áº£nh, ghi file.
10. Worker táº¡o `DownloadResult`.
11. `DownloadJobTracker` nháº­n káº¿t quáº£ vÃ  cáº­p nháº­t task thÃ nh `SUCCESS` hoáº·c `FAILED`.
12. Khi client gá»i `GET /jobs/{jobId}`, tracker tráº£ snapshot tá»•ng quan.
13. Khi client gá»i `GET /jobs/{jobId}/tasks`, tracker tráº£ danh sÃ¡ch snapshot chi tiáº¿t cá»§a tá»«ng task.

## 8. PhÃ¢n biá»‡t 4 khÃ¡i niá»‡m ráº¥t dá»… bá»‹ trá»™n

### `job`

Má»™t Ä‘á»£t cÃ´ng viá»‡c lá»›n.

VÃ­ dá»¥:

- táº£i 50 áº£nh tá»« má»™t nguá»“n

### `task`

Má»™t Ä‘Æ¡n vá»‹ cÃ´ng viá»‡c nhá» bÃªn trong job.

VÃ­ dá»¥:

- táº£i áº£nh sá»‘ 17

### `thread`

Worker Ä‘ang thá»±c thi task.

VÃ­ dá»¥:

- `IO-Lab-Thread-7`

### `executor`

NÆ¡i quáº£n lÃ½ nhÃ³m worker thread.

VÃ­ dá»¥:

- `imageTaskExecutor`

CÃ¡ch nhá»›:

- `job` = chiáº¿n dá»‹ch lá»›n
- `task` = viá»‡c con
- `thread` = ngÆ°á»i lÃ m viá»‡c
- `executor` = Ä‘á»™i ngÆ°á»i lÃ m viá»‡c

## 9. VÃ¬ sao thiáº¿t káº¿ nÃ y Ä‘Ã¡ng giÃ¡?

Náº¿u chá»‰ muá»‘n demo táº£i áº£nh, báº¡n khÃ´ng nháº¥t thiáº¿t pháº£i cÃ³ `job` vÃ  `task`.

NhÆ°ng náº¿u muá»‘n project lá»›n lÃªn thÃ nh engine tháº­t sá»±, thÃ¬ cÃ¡c khÃ¡i niá»‡m nÃ y lÃ  ná»n táº£ng Ä‘á»ƒ sau nÃ y thÃªm:

- retry
- timeout policy
- cancel / pause / resume
- persistence
- metrics
- benchmark report
- source adapters

NÃ³i ngáº¯n gá»n:

- khÃ´ng cÃ³ `job/task` thÃ¬ báº¡n chá»‰ cÃ³ má»™t Ä‘á»‘ng lá»i gá»i async rá»i ráº¡c
- cÃ³ `job/task` thÃ¬ báº¡n cÃ³ má»™t workflow cÃ³ thá»ƒ quan sÃ¡t, giáº£i thÃ­ch, vÃ  má»Ÿ rá»™ng

## 10. CÃ¡ch Ä‘á»c code cho Ä‘á»¡ rá»‘i

Báº¡n nÃªn Ä‘á»c code theo thá»© tá»± nÃ y:

1. `DownloadController`
2. `DownloadJobService`
3. `ImageDownloaderService`
4. `DownloadJobTracker`
5. `AsyncConfig`

Náº¿u Ä‘á»c theo thá»© tá»± Ä‘Ã³, báº¡n sáº½ tháº¥y rÃµ:

- request vÃ o á»Ÿ Ä‘Ã¢u
- ai chia viá»‡c
- ai lÃ m viá»‡c tháº­t
- ai ghi nháº­n tráº¡ng thÃ¡i
- ai cáº¥u hÃ¬nh thread pool