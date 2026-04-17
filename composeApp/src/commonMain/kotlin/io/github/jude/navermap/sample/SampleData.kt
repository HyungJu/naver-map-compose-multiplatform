package io.github.hyungju.navermap.sample

import androidx.compose.ui.graphics.Color
import io.github.hyungju.navermap.compose.CameraPosition
import io.github.hyungju.navermap.compose.LatLng

internal data class DynamicMarkerStation(
    val id: String,
    val name: String,
    val brand: String,
    val position: LatLng,
    val accentColor: Color,
    val basePrice: Int,
    val step: Int,
)

internal data class NationwideMarkerSample(
    val id: String,
    val badge: String,
    val name: String,
    val category: String,
    val position: LatLng,
    val accentColor: Color,
)

internal data class RegionFocus(
    val label: String,
    val cameraPosition: CameraPosition,
)

internal val renderKeyStations = listOf(
    DynamicMarkerStation(
        id = "city-hall",
        name = "서울시청",
        brand = "도심",
        position = LatLng(37.56661, 126.97839),
        accentColor = Color(0xFF2563EB),
        basePrice = 1698,
        step = 9,
    ),
    DynamicMarkerStation(
        id = "gwanghwamun",
        name = "광화문",
        brand = "북촌",
        position = LatLng(37.57596, 126.97686),
        accentColor = Color(0xFF0F766E),
        basePrice = 1712,
        step = 7,
    ),
    DynamicMarkerStation(
        id = "yeouido",
        name = "여의도",
        brand = "강변",
        position = LatLng(37.52193, 126.92462),
        accentColor = Color(0xFF9333EA),
        basePrice = 1685,
        step = 11,
    ),
    DynamicMarkerStation(
        id = "hongdae",
        name = "홍대입구",
        brand = "서교",
        position = LatLng(37.55635, 126.92204),
        accentColor = Color(0xFFEA580C),
        basePrice = 1679,
        step = 8,
    ),
    DynamicMarkerStation(
        id = "jamsil",
        name = "잠실",
        brand = "동남",
        position = LatLng(37.51326, 127.10013),
        accentColor = Color(0xFFDC2626),
        basePrice = 1725,
        step = 6,
    ),
    DynamicMarkerStation(
        id = "pangyo",
        name = "판교",
        brand = "테크",
        position = LatLng(37.39482, 127.11118),
        accentColor = Color(0xFF4F46E5),
        basePrice = 1703,
        step = 10,
    ),
)

internal val nationwideRegionFocuses = listOf(
    RegionFocus(
        label = "전국",
        cameraPosition = CameraPosition(
            target = LatLng(36.25, 127.85),
            zoom = 6.7,
        ),
    ),
    RegionFocus(
        label = "수도권",
        cameraPosition = CameraPosition(
            target = LatLng(37.48, 126.95),
            zoom = 8.1,
        ),
    ),
    RegionFocus(
        label = "충청·호남",
        cameraPosition = CameraPosition(
            target = LatLng(36.02, 126.95),
            zoom = 7.2,
        ),
    ),
    RegionFocus(
        label = "영남",
        cameraPosition = CameraPosition(
            target = LatLng(35.63, 128.85),
            zoom = 7.3,
        ),
    ),
    RegionFocus(
        label = "제주",
        cameraPosition = CameraPosition(
            target = LatLng(33.3846, 126.5535),
            zoom = 9.1,
        ),
    ),
)

private val seoulColor = Color(0xFF2563EB)
private val incheonColor = Color(0xFF0891B2)
private val gyeonggiColor = Color(0xFF4F46E5)
private val gangwonColor = Color(0xFF0F766E)
private val chungcheongColor = Color(0xFF7C3AED)
private val jeonbukColor = Color(0xFF256F3A)
private val jeonnamColor = Color(0xFF0E7490)
private val daeguColor = Color(0xFFBE123C)
private val gyeongbukColor = Color(0xFFB45309)
private val busanColor = Color(0xFFDB2777)
private val ulsanColor = Color(0xFF4338CA)
private val gyeongnamColor = Color(0xFFEA580C)
private val jejuColor = Color(0xFFCA8A04)

internal val nationwideMarkers = listOf(
    NationwideMarkerSample("seoul-cityhall", "서울", "시청", "행정", LatLng(37.56661, 126.97839), seoulColor),
    NationwideMarkerSample("seoul-gwanghwamun", "서울", "광화문", "광장", LatLng(37.57596, 126.97686), seoulColor),
    NationwideMarkerSample("seoul-gangnam", "서울", "강남역", "상권", LatLng(37.49794, 127.02762), seoulColor),
    NationwideMarkerSample("seoul-jamsil", "서울", "잠실", "스포츠", LatLng(37.51326, 127.10013), seoulColor),
    NationwideMarkerSample("seoul-hongdae", "서울", "홍대", "문화", LatLng(37.55635, 126.92204), seoulColor),
    NationwideMarkerSample("seoul-yeouido", "서울", "여의도", "금융", LatLng(37.52193, 126.92462), seoulColor),
    NationwideMarkerSample("incheon-songdo", "인천", "송도", "신도시", LatLng(37.38330, 126.65644), incheonColor),
    NationwideMarkerSample("incheon-airport", "인천", "공항", "교통", LatLng(37.46019, 126.44070), incheonColor),
    NationwideMarkerSample("gyeonggi-suwon", "경기", "수원", "행정", LatLng(37.26357, 127.02860), gyeonggiColor),
    NationwideMarkerSample("gyeonggi-pangyo", "경기", "판교", "테크", LatLng(37.39482, 127.11118), gyeonggiColor),
    NationwideMarkerSample("gyeonggi-goyang", "경기", "고양", "주거", LatLng(37.65836, 126.83202), gyeonggiColor),
    NationwideMarkerSample("gyeonggi-yongin", "경기", "용인", "관광", LatLng(37.24109, 127.17755), gyeonggiColor),
    NationwideMarkerSample("gyeonggi-paju", "경기", "파주", "출판", LatLng(37.75987, 126.78018), gyeonggiColor),
    NationwideMarkerSample("gyeonggi-anyang", "경기", "안양", "산업", LatLng(37.39429, 126.95675), gyeonggiColor),
    NationwideMarkerSample("gangwon-chuncheon", "강원", "춘천", "호수", LatLng(37.88132, 127.72976), gangwonColor),
    NationwideMarkerSample("gangwon-wonju", "강원", "원주", "혁신", LatLng(37.34222, 127.92016), gangwonColor),
    NationwideMarkerSample("gangwon-gangneung", "강원", "강릉", "해변", LatLng(37.75185, 128.87606), gangwonColor),
    NationwideMarkerSample("gangwon-sokcho", "강원", "속초", "관광", LatLng(38.20431, 128.59120), gangwonColor),
    NationwideMarkerSample("gangwon-donghae", "강원", "동해", "항만", LatLng(37.52472, 129.11429), gangwonColor),
    NationwideMarkerSample("chungcheong-daejeon", "충청", "대전", "과학", LatLng(36.35041, 127.38455), chungcheongColor),
    NationwideMarkerSample("chungcheong-sejong", "충청", "세종", "행정", LatLng(36.48001, 127.28903), chungcheongColor),
    NationwideMarkerSample("chungcheong-cheongju", "충청", "청주", "공항", LatLng(36.64243, 127.48903), chungcheongColor),
    NationwideMarkerSample("chungcheong-chungju", "충청", "충주", "호반", LatLng(36.99101, 127.92595), chungcheongColor),
    NationwideMarkerSample("chungcheong-cheonan", "충청", "천안", "교통", LatLng(36.81511, 127.11389), chungcheongColor),
    NationwideMarkerSample("chungcheong-asan", "충청", "아산", "온천", LatLng(36.78979, 127.00185), chungcheongColor),
    NationwideMarkerSample("chungcheong-boryeong", "충청", "보령", "축제", LatLng(36.33353, 126.61272), chungcheongColor),
    NationwideMarkerSample("chungcheong-gongju", "충청", "공주", "백제", LatLng(36.45500, 127.12474), chungcheongColor),
    NationwideMarkerSample("jeonbuk-jeonju", "전북", "전주", "한옥", LatLng(35.82417, 127.14800), jeonbukColor),
    NationwideMarkerSample("jeonbuk-gunsan", "전북", "군산", "항구", LatLng(35.96763, 126.73681), jeonbukColor),
    NationwideMarkerSample("jeonbuk-iksan", "전북", "익산", "철도", LatLng(35.94829, 126.95760), jeonbukColor),
    NationwideMarkerSample("jeonbuk-namwon", "전북", "남원", "관광", LatLng(35.41636, 127.39039), jeonbukColor),
    NationwideMarkerSample("jeonnam-gwangju", "전남", "광주", "광역", LatLng(35.15955, 126.85260), jeonnamColor),
    NationwideMarkerSample("jeonnam-mokpo", "전남", "목포", "항만", LatLng(34.81183, 126.39217), jeonnamColor),
    NationwideMarkerSample("jeonnam-suncheon", "전남", "순천", "정원", LatLng(34.95058, 127.48721), jeonnamColor),
    NationwideMarkerSample("jeonnam-yeosu", "전남", "여수", "바다", LatLng(34.76037, 127.66222), jeonnamColor),
    NationwideMarkerSample("jeonnam-naju", "전남", "나주", "혁신", LatLng(35.01506, 126.71076), jeonnamColor),
    NationwideMarkerSample("jeonnam-boseong", "전남", "보성", "차밭", LatLng(34.77145, 127.08016), jeonnamColor),
    NationwideMarkerSample("daegu-dongseongno", "대구", "동성로", "상권", LatLng(35.86939, 128.59342), daeguColor),
    NationwideMarkerSample("daegu-suseong", "대구", "수성못", "호수", LatLng(35.82784, 128.62001), daeguColor),
    NationwideMarkerSample("gyeongbuk-pohang", "경북", "포항", "철강", LatLng(36.01902, 129.34348), gyeongbukColor),
    NationwideMarkerSample("gyeongbuk-gyeongju", "경북", "경주", "문화재", LatLng(35.85617, 129.22475), gyeongbukColor),
    NationwideMarkerSample("gyeongbuk-andong", "경북", "안동", "하회", LatLng(36.56843, 128.72936), gyeongbukColor),
    NationwideMarkerSample("gyeongbuk-gumi", "경북", "구미", "산업", LatLng(36.11953, 128.34457), gyeongbukColor),
    NationwideMarkerSample("gyeongbuk-gimcheon", "경북", "김천", "교통", LatLng(36.13983, 128.11363), gyeongbukColor),
    NationwideMarkerSample("gyeongbuk-ulleung", "경북", "울릉", "도서", LatLng(37.48442, 130.90570), gyeongbukColor),
    NationwideMarkerSample("busan-haeundae", "부산", "해운대", "해변", LatLng(35.16317, 129.16356), busanColor),
    NationwideMarkerSample("busan-seomyeon", "부산", "서면", "상권", LatLng(35.15773, 129.05918), busanColor),
    NationwideMarkerSample("busan-nampo", "부산", "남포", "관광", LatLng(35.09789, 129.03674), busanColor),
    NationwideMarkerSample("ulsan-center", "울산", "울산", "산업", LatLng(35.53838, 129.31136), ulsanColor),
    NationwideMarkerSample("gyeongnam-changwon", "경남", "창원", "행정", LatLng(35.22806, 128.68111), gyeongnamColor),
    NationwideMarkerSample("gyeongnam-jinhae", "경남", "진해", "군항", LatLng(35.14921, 128.65917), gyeongnamColor),
    NationwideMarkerSample("gyeongnam-jinju", "경남", "진주", "혁신", LatLng(35.17955, 128.10763), gyeongnamColor),
    NationwideMarkerSample("gyeongnam-tongyeong", "경남", "통영", "항만", LatLng(34.85439, 128.43320), gyeongnamColor),
    NationwideMarkerSample("gyeongnam-geoje", "경남", "거제", "조선", LatLng(34.88062, 128.62175), gyeongnamColor),
    NationwideMarkerSample("gyeongnam-sacheon", "경남", "사천", "항공", LatLng(35.00381, 128.06483), gyeongnamColor),
    NationwideMarkerSample("gyeongnam-miryang", "경남", "밀양", "관문", LatLng(35.50376, 128.74644), gyeongnamColor),
    NationwideMarkerSample("jeju-city", "제주", "제주시", "도심", LatLng(33.49962, 126.53119), jejuColor),
    NationwideMarkerSample("jeju-seogwipo", "제주", "서귀포", "해안", LatLng(33.25301, 126.56000), jejuColor),
    NationwideMarkerSample("jeju-seongsan", "제주", "성산", "일출", LatLng(33.45894, 126.94248), jejuColor),
)
