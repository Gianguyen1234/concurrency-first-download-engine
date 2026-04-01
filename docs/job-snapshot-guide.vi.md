# HÆ°á»›ng Dáº«n Äá»c Job Snapshot

TÃ i liá»‡u nÃ y giáº£i thÃ­ch cÃ¡ch Ä‘á»c response cá»§a `GET /jobs/{jobId}` trong project.

## ÄÃ¢y lÃ  áº£nh chá»¥p táº¡i thá»i Ä‘iá»ƒm gá»i API

Snapshot cá»§a job lÃ  tráº¡ng thÃ¡i táº¡i **Ä‘Ãºng thá»i Ä‘iá»ƒm** báº¡n gá»i endpoint.

NÃ³ khÃ´ng nháº¥t thiáº¿t lÃ  káº¿t quáº£ cuá»‘i cÃ¹ng.

VÃ­ dá»¥:

```json
{
  "jobId": 1001,
  "sourceBaseUrl": "https://picsum.photos/300/300",
  "totalRequested": 50,
  "queued": 0,
  "running": 8,
  "completed": 42,
  "okCount": 41,
  "failCount": 1,
  "totalBytes": 633282,
  "totalWallMillis": 22501,
  "avgTaskMillis": 2821.9523809523807,
  "throughputImagesPerSecond": 1.8221412381671924,
  "throughputMegabytesPerSecond": 0.026840797228673517,
  "finished": false
}
```

## Ã nghÄ©a tá»«ng thÃ´ng sá»‘

### `queued`

Sá»‘ task cÃ²n Ä‘ang náº±m trong hÃ ng Ä‘á»£i, chÆ°a Ä‘Æ°á»£c worker láº¥y ra cháº¡y.

VÃ­ dá»¥ `queued: 0` nghÄ©a lÃ :

- khÃ´ng cÃ²n task nÃ o chá» trong queue cá»§a job
- cÃ¡c task Ä‘Ã£ Ä‘Æ°á»£c láº¥y ra háº¿t Ä‘á»ƒ xá»­ lÃ½, hoáº·c Ä‘Ã£ xong

### `running`

Sá»‘ task Ä‘ang cháº¡y táº¡i thá»i Ä‘iá»ƒm chá»¥p snapshot.

VÃ­ dá»¥ `running: 8` nghÄ©a lÃ  cÃ³ 8 task Ä‘ang:

- chá» máº¡ng
- nháº­n bytes
- ghi file
- hoáº·c Ä‘ang á»Ÿ giá»¯a quÃ¡ trÃ¬nh xá»­ lÃ½

### `completed`

Sá»‘ task Ä‘Ã£ káº¿t thÃºc.

LÆ°u Ã½:

- `completed` bao gá»“m cáº£ `SUCCESS` vÃ  `FAILED`

VÃ­ dá»¥ `completed: 42` nghÄ©a lÃ  Ä‘Ã£ cÃ³ 42 task xong viá»‡c.

### `okCount`

Sá»‘ task thÃ nh cÃ´ng trong nhÃ³m task Ä‘Ã£ hoÃ n thÃ nh.

VÃ­ dá»¥ `okCount: 41` nghÄ©a lÃ  trong 42 task Ä‘Ã£ xong, cÃ³ 41 task thÃ nh cÃ´ng.

### `failCount`

Sá»‘ task tháº¥t báº¡i trong nhÃ³m task Ä‘Ã£ hoÃ n thÃ nh.

VÃ­ dá»¥ `failCount: 1` nghÄ©a lÃ  trong 42 task Ä‘Ã£ xong, cÃ³ 1 task lá»—i.

### `totalBytes`

Tá»•ng sá»‘ byte táº£i thÃ nh cÃ´ng Ä‘Æ°á»£c tÃ­nh Ä‘áº¿n thá»i Ä‘iá»ƒm hiá»‡n táº¡i.

LÆ°u Ã½:

- chá»‰ cá»™ng cÃ¡c task `SUCCESS`
- task `FAILED` khÃ´ng Ä‘Æ°á»£c cá»™ng vÃ o Ä‘Ã¢y

### `totalWallMillis`

Thá»i gian thá»±c ngoÃ i Ä‘á»i cá»§a job, tÃ­nh tá»« lÃºc báº¯t Ä‘áº§u job Ä‘áº¿n lÃºc chá»¥p snapshot.

VÃ­ dá»¥:

- `22501 ms` xáº¥p xá»‰ `22.5 giÃ¢y`

ÄÃ¢y lÃ  **wall-clock time**, khÃ´ng pháº£i tá»•ng thá»i gian cá»™ng dá»“n cá»§a tá»«ng task.

### `avgTaskMillis`

Thá»i gian trung bÃ¬nh cá»§a cÃ¡c task Ä‘Ã£ hoÃ n thÃ nh.

CÃ´ng thá»©c hiá»‡n táº¡i:

- láº¥y tá»•ng `millis` cá»§a cÃ¡c task Ä‘Ã£ xong
- chia cho `completed`

VÃ­ dá»¥:

- `avgTaskMillis = 2821.95 ms`
- xáº¥p xá»‰ `2.82 giÃ¢y/task`

### `throughputImagesPerSecond`

ThÃ´ng lÆ°á»£ng theo sá»‘ áº£nh thÃ nh cÃ´ng trÃªn má»—i giÃ¢y.

CÃ´ng thá»©c gáº§n Ä‘Ãºng:

- `okCount / totalWallSeconds`

VÃ­ dá»¥:

- `41 / 22.501 ~= 1.82 áº£nh/giÃ¢y`

### `throughputMegabytesPerSecond`

ThÃ´ng lÆ°á»£ng theo MB dá»¯ liá»‡u táº£i thÃ nh cÃ´ng trÃªn má»—i giÃ¢y.

NÃ³ tráº£ lá»i cÃ¢u há»i:

- há»‡ thá»‘ng Ä‘ang táº£i Ä‘Æ°á»£c bao nhiÃªu MB má»—i giÃ¢y?

### `finished`

Cho biáº¿t job Ä‘Ã£ xong toÃ n bá»™ hay chÆ°a.

VÃ­ dá»¥ `finished: false` nghÄ©a lÃ :

- job váº«n chÆ°a xong
- vÃ¬ `completed < totalRequested`
- vÃ  váº«n cÃ²n task Ä‘ang `running`

## CÃ¡ch Ä‘á»c nhanh cáº£ cá»¥m thÃ´ng sá»‘

Vá»›i snapshot á»Ÿ trÃªn, báº¡n cÃ³ thá»ƒ Ä‘á»c nhÆ° sau:

- Job Ä‘Ã£ xá»­ lÃ½ xong `42/50` task
- Trong sá»‘ Ä‘Ã³ `41` thÃ nh cÃ´ng, `1` tháº¥t báº¡i
- KhÃ´ng cÃ²n task nÃ o chá» trong queue
- Váº«n cÃ²n `8` task Ä‘ang cháº¡y
- ÄÃ£ táº£i Ä‘Æ°á»£c khoáº£ng `633 KB`
- Job Ä‘Ã£ cháº¡y khoáº£ng `22.5 giÃ¢y`
- Tá»‘c Ä‘á»™ hiá»‡n táº¡i khoáº£ng `1.82 áº£nh/giÃ¢y`
- Job chÆ°a hoÃ n táº¥t

## Hai cÃ´ng thá»©c ráº¥t dá»… nhá»›

### CÃ´ng thá»©c 1

`completed = okCount + failCount`

VÃ­ dá»¥:

- `42 = 41 + 1`

### CÃ´ng thá»©c 2

`totalRequested ~= queued + running + completed`

VÃ­ dá»¥:

- `50 = 0 + 8 + 42`

Hai cÃ´ng thá»©c nÃ y ráº¥t há»¯u Ã­ch Ä‘á»ƒ tá»± kiá»ƒm tra snapshot cÃ³ há»£p lÃ½ hay khÃ´ng.

## Chá»— dá»… nháº§m nháº¥t: `avgTaskMillis` vÃ  `totalWallMillis`

Hai sá»‘ nÃ y **khÃ´ng cÃ¹ng nghÄ©a**.

### `avgTaskMillis`

Äo Ä‘á»™ trá»… trung bÃ¬nh cá»§a tá»«ng task Ä‘Ã£ hoÃ n thÃ nh.

ÄÃ¢y lÃ  gÃ³c nhÃ¬n theo **task riÃªng láº»**.

### `totalWallMillis`

Äo thá»i gian thá»±c ngoÃ i Ä‘á»i cá»§a **cáº£ job**.

ÄÃ¢y lÃ  gÃ³c nhÃ¬n theo **toÃ n bá»™ quÃ¡ trÃ¬nh**.

VÃ¬ task cháº¡y song song nÃªn:

- wall time khÃ´ng báº±ng tá»•ng thá»i gian cá»§a tá»«ng task cá»™ng láº¡i

ÄÃ¢y lÃ  dáº¥u hiá»‡u cá»§a concurrency.

## VÃ¬ sao `avgTaskMillis` cÃ³ thá»ƒ cao nhÆ°ng `throughput` váº«n khÃ¡?

VÃ¬ hai sá»‘ nÃ y Ä‘o hai thá»© khÃ¡c nhau:

- `avgTaskMillis`: latency cá»§a tá»«ng task
- `throughput`: nÄƒng suáº¥t toÃ n há»‡ thá»‘ng

## VÃ­ dá»¥ Ä‘á»ƒ hiá»ƒu

Giáº£ sá»­ cÃ³ 3 task, má»—i task máº¥t 3 giÃ¢y.

### Náº¿u cháº¡y tuáº§n tá»±

- task 1: 3s
- task 2: 3s
- task 3: 3s

Tá»•ng máº¥t 9s.

Throughput:

- `3 áº£nh / 9 giÃ¢y = 0.33 áº£nh/giÃ¢y`

Avg task:

- váº«n lÃ  `3 giÃ¢y/task`

### Náº¿u cháº¡y song song

- cáº£ 3 cÃ¹ng báº¯t Ä‘áº§u
- sau 3 giÃ¢y, cáº£ 3 cÃ¹ng xong

Throughput:

- `3 áº£nh / 3 giÃ¢y = 1 áº£nh/giÃ¢y`

Avg task:

- váº«n lÃ  `3 giÃ¢y/task`

Káº¿t luáº­n:

- `avgTaskMillis` khÃ´ng Ä‘á»•i
- `throughput` tÄƒng máº¡nh vÃ¬ cÃ³ song song

## Táº¡i sao Ä‘iá»u nÃ y há»£p lÃ½ trong bÃ i toÃ¡n I/O-bound?

Vá»›i I/O-bound:

- task sá»‘ng khÃ¡ lÃ¢u
- nhÆ°ng pháº§n lá»›n thá»i gian lÃ  Ä‘ang **chá»**

VÃ­ dá»¥ má»™t task download áº£nh máº¥t 2.8 giÃ¢y:

- má»™t pháº§n nhá» lÃ  code Java thá»±c sá»± cháº¡y
- pháº§n lá»›n cÃ²n láº¡i lÃ :
  - chá» DNS
  - chá» káº¿t ná»‘i TCP
  - chá» server tráº£ response
  - chá» bytes Ä‘i qua máº¡ng
  - chá» ghi file

Khi task A Ä‘ang chá» I/O, task B, C, D váº«n cÃ³ thá»ƒ cháº¡y song song trÃªn cÃ¡c thread khÃ¡c.

VÃ¬ váº­y:

- latency cá»§a tá»«ng task cÃ³ thá»ƒ váº«n cao
- throughput tá»•ng thá»ƒ váº«n Ä‘Æ°á»£c cáº£i thiá»‡n nhiá»u

ÄÃ³ lÃ  lÃ½ do thread pool ráº¥t há»£p vá»›i bÃ i toÃ¡n I/O-bound.

## CÃ¡ch nÃ³i ngáº¯n gá»n 

Báº¡n cÃ³ thá»ƒ nÃ³i:

> Trong há»‡ thá»‘ng concurrent, `avgTaskMillis` vÃ  `throughput` lÃ  hai chá»‰ sá»‘ khÃ¡c nhau. Má»—i task cÃ³ thá»ƒ máº¥t vÃ i giÃ¢y vÃ¬ pháº£i chá» I/O, nhÆ°ng nhiá»u task cháº¡y song song nÃªn há»‡ thá»‘ng váº«n hoÃ n thÃ nh Ä‘Æ°á»£c nhiá»u áº£nh má»—i giÃ¢y.

Hoáº·c ngáº¯n gá»n hÆ¡n:

> Má»™t áº£nh riÃªng láº» váº«n cháº­m, nhÆ°ng nhiá»u áº£nh cháº¡y cÃ¹ng lÃºc nÃªn tá»•ng nÄƒng suáº¥t cá»§a há»‡ thá»‘ng váº«n cao.