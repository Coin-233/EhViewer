#[cfg(test)]
mod tests {
    use crate::parser::api::parse_vote_tag;
    use crate::parser::archive::{parse_archive_url, parse_archives_with_funds};
    use crate::parser::detail::{parse_gallery_detail, parse_preview_list};
    use crate::parser::list::{parse_info_list, parse_token_and_gid};
    use crate::parser::profile::{parse_profile, parse_profile_url};
    use reqwest::get;
    use std::fs;
    use tl::ParserOptions;

    #[test]
    fn test_proxy_gallery_urls() {
        for path in ["g", "mpv", "lofi/g", "lofi/mpv"] {
            let url = format!("https://exht.sakuras.in/{path}/530350/8b3c7e4a21/");
            assert_eq!(parse_token_and_gid(&url), Some((530350, "8b3c7e4a21".to_string())));
        }
    }

    #[test]
    fn test_proxy_preview_url() {
        let body = r#"<div id="gdt"><a href="https://exht.sakuras.in/s/8b3c7e4a21/530350-2">
            <div title="Page 2" style="width:100px;height:150px;background:url(https://exht.sakuras.in/thumb.jpg)"></div>
            </a></div>"#;
        let dom = tl::parse(body, ParserOptions::default()).unwrap();
        let previews = parse_preview_list(&dom, dom.parser()).unwrap();
        assert_eq!(previews.len(), 1);
        assert_eq!(previews[0].1.pToken, "8b3c7e4a21");
        assert_eq!(previews[0].1.position, 1);
    }

    #[test]
    fn test_proxy_gallery_detail() {
        for (uid, key, expected_uid, expected_key) in [("\"hidden\"", "hidden", 0, None), ("0", "", 0, None), ("123", "deadbeef", 123, Some("deadbeef"))] {
            let body = format!(
                r#"<script>var gid = 530350; var token = "8b3c7e4a21"; var apiuid = {uid}; var apikey = "{key}"; var average_rating = 4.5;</script>
                <div id="gdt"></div><table id="taglist"></table><div id="cdiv"><div id="chd"></div></div>
                <div id="gd5"><a onclick="noop()">Archive</a><a onclick="noop()">Torrents (0)</a></div>
                <div id="gd1"><div style="background:url(https://exht.sakuras.in/thumb.jpg)"></div></div>
                <div id="gn">Proxy&nbsp;&times;</div><div id="gj"></div><div id="gdc">漫画</div>
                <table id="gdd"></table><span id="rating_count">1</span><div id="fav"></div>"#
            );
            let mut dom = tl::parse(&body, ParserOptions::default().track_ids()).unwrap();
            let detail = parse_gallery_detail(&mut dom, &body).unwrap();
            assert_eq!(detail.apiUid, expected_uid);
            assert_eq!(detail.apiKey.as_deref(), expected_key);
            assert_eq!(detail.galleryInfo.title, "Proxy ×");
            assert_eq!(detail.galleryInfo.category, 0x4);
        }
    }

    #[tokio::test]
    async fn test_parse_info() {
        let resp = get("https://exht.sakuras.in/").await.expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_info_list(&dom, dom.parser()).expect("Failed to parse info list");
        dbg!(result);
    }

    #[test]
    fn test_parse_archives_with_funds() {
        let body = fs::read_to_string("test_data/archives_with_funds.html").expect("Failed to read html file");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_archives_with_funds(&dom, dom.parser(), &body).expect("Failed to parse archives");
        dbg!(result);
    }

    #[test]
    fn test_parse_archive_url() {
        let body = fs::read_to_string("test_data/archive_url.html").expect("Failed to read html file");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_archive_url(&dom, dom.parser(), &body).expect("Failed to parse archives");
        assert_eq!(result, Some("https://0?start=1".to_string()));
    }

    #[tokio::test]
    async fn test_parse_profile() {
        let body = fs::read_to_string("test_data/profile_url.html").expect("Failed to read html file");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_profile_url(&dom, dom.parser()).expect("Failed to parse profile url");
        let resp = get(result).await.expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_profile(&dom, dom.parser()).expect("Failed to parse profile");
        assert_eq!(result.displayName, "Tenboro".to_string());
        assert_eq!(result.avatar, Some("https://forums.exht.sakuras.in/ehgt/jdk_180.png".to_string()));
    }

    #[tokio::test]
    async fn test_parse_gallery_detail() {
        let resp = get("https://exht.sakuras.in/g/530350/8b3c7e4a21/").await.expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let mut dom = tl::parse(&body, ParserOptions::default().track_ids()).expect("Failed to parse HTML");
        let result = parse_gallery_detail(&mut dom, &body).expect("Failed to parse gallery detail");
        dbg!(result);
    }

    #[test]
    fn test_parse_tag_gallery() {
        let json = fs::read_to_string("test_data/vote_tag.json").expect("Failed to read json file");
        let result = parse_vote_tag(&json).expect_err("Should fail to parse");
        dbg!(result);
    }
}
