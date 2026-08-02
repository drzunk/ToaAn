package vn.tuphap.automation.pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

/**
 * Điền dropdown custom trong eform nhúng ({@code button.inp} — "— Chọn —").
 * Diff text trước/sau click; xác nhận button đã đổi giá trị.
 */
final class EformDropdownHelper {

    private EformDropdownHelper() {
    }

    static int fillPending(WebDriver driver, WebUI webUI) {
        int filled = 0;
        int guard = 0;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        while (guard++ < 10) {
            Object pendingBefore = js.executeScript(COUNT_PENDING_JS);
            int pending = pendingBefore == null ? 0 : ((Number) pendingBefore).intValue();
            if (pending <= 0) {
                break;
            }

            Object before = js.executeScript(SNAPSHOT_VISIBLE_TEXTS_JS);
            Object opened = js.executeScript(OPEN_FIRST_PENDING_DROPDOWN_JS);
            if (opened == null) {
                break;
            }
            webUI.sleepMillis(550);

            // Nếu có ô tìm trong menu — gõ ký tự để hiện option (tránh "Không tìm thấy").
            // JS trả 'NO_SEARCH' khi dropdown không có ô tìm: khi đó không có gì để lọc nên
            // khỏi nghỉ chờ kết quả lọc.
            Object typed = js.executeScript(TYPE_IN_OPEN_SEARCH_JS, "a");
            if (!"NO_SEARCH".equals(String.valueOf(typed))) {
                webUI.sleepMillis(350);
            }

            String chosen = tryPick(js, before);
            if (chosen == null) {
                webUI.switchToDefaultContent();
                try {
                    chosen = tryPick(js, before);
                    if (chosen != null) {
                        logChosen(chosen, true);
                    } else {
                        System.out.println(" ⚠ Portal parent: không có option mới.");
                    }
                } finally {
                    try {
                        webUI.switchToIframe(NoiDungDonPage.IFRAME_NOI_DUNG);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                logChosen(chosen, false);
            }

            webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
            Object confirmed = js.executeScript(CONFIRM_OR_SKIP_JS);
            if ("OK".equals(String.valueOf(confirmed))) {
                filled++;
            } else {
                // Keyboard fallback nếu click option không dính vào button.
                Object kb = js.executeScript(KEYBOARD_SELECT_OPEN_DROPDOWN_JS);
                webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
                Object confirmed2 = js.executeScript(CONFIRM_OR_SKIP_JS);
                if ("OK".equals(String.valueOf(confirmed2))) {
                    System.out.println(" ➔ Chọn dropdown eform (keyboard): '" + kb + "'");
                    filled++;
                } else {
                    System.out.println(" ⚠ Dropdown eform bỏ qua (không đổi giá trị) — " + confirmed
                            + " open=" + opened + " chosen=" + chosen);
                    js.executeScript(FORCE_SKIP_OPEN_JS);
                }
            }
        }
        return filled;
    }

    private static String tryPick(JavascriptExecutor js, Object beforeSnapshot) {
        Object picked = js.executeScript(PICK_NEW_OPTION_JS, beforeSnapshot);
        if (picked == null) {
            return null;
        }
        String s = String.valueOf(picked);
        if (!s.startsWith("OK:")) {
            System.out.println(" ⚠ pick: " + s);
            return null;
        }
        String body = s.substring(3);
        int cut = body.indexOf('|');
        return cut >= 0 ? body.substring(0, cut) : body;
    }

    private static void logChosen(String chosen, boolean portal) {
        String where = portal ? " (portal parent)" : "";
        System.out.println(" ➔ Chọn dropdown eform" + where + ": '" + abbreviate(chosen, 60) + "'");
        TestActionLog.chon("Dropdown eform (iframe)", chosen);
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static final String COUNT_PENDING_JS =
            "var isPlaceholder=function(t){"
                    + "t=(t||'').replace(/\\u00a0/g,' ').trim().toLowerCase();"
                    + "return !t || t.indexOf('chọn')>=0 || t==='▾' || t.charAt(0)==='—' || t.charAt(0)==='-';"
                    + "};"
                    + "return [...document.querySelectorAll('button.inp')].filter(b=>"
                    + "!b.disabled && b.getAttribute('data-skip-dd')!=='1' && isPlaceholder(b.innerText||b.textContent)).length;";

    private static final String SNAPSHOT_VISIBLE_TEXTS_JS =
            "var out=[]; var walk=function(n){if(!n||n.nodeType!==1)return; if(n.shadowRoot)walk(n.shadowRoot);"
                    + " out.push(n); var c=n.children||[]; for(var i=0;i<c.length;i++) walk(c[i]);};"
                    + "walk(document.documentElement); var set={};"
                    + "out.forEach(function(el){try{"
                    + " var st=getComputedStyle(el); if(st.display==='none'||st.visibility==='hidden')return;"
                    + " var t=(el.innerText||'').trim(); if(!t||t.length>80||t.indexOf('\\n')>=0)return;"
                    + " set[t]=1;}catch(e){}});"
                    + "return Object.keys(set);";

    private static final String OPEN_FIRST_PENDING_DROPDOWN_JS =
            "var isPlaceholder=function(t){"
                    + "t=(t||'').replace(/\\u00a0/g,' ').trim().toLowerCase();"
                    + "return !t || t.indexOf('chọn')>=0 || t==='▾' || t.charAt(0)==='—' || t.charAt(0)==='-';"
                    + "};"
                    + "[...document.querySelectorAll('button.inp')].forEach(function(b){b.removeAttribute('data-dd-open');});"
                    + "var btns=[...document.querySelectorAll('button.inp')].filter(b=>{"
                    + "  if(b.getAttribute('data-skip-dd')==='1') return false;"
                    + "  if(b.disabled) return false;"
                    + "  return isPlaceholder(b.innerText||b.textContent);"
                    + "});"
                    + "if(!btns.length) return null;"
                    + "var btn=btns[0];"
                    + "btn.setAttribute('data-dd-open','1');"
                    + "btn.scrollIntoView({block:'center'});"
                    + "btn.focus();"
                    + "btn.click();"
                    + "return 'OPEN:'+(btn.innerText||'').trim();";

    private static final String TYPE_IN_OPEN_SEARCH_JS =
            "var q=(arguments[0]||'a');"
                    + "var inputs=[...document.querySelectorAll('input[type=search],input[type=text],input:not([type])')]"
                    + ".filter(function(i){"
                    + "  try{ var st=getComputedStyle(i); if(st.display==='none'||st.visibility==='hidden')return false;"
                    + "  var r=i.getBoundingClientRect(); return r.width>40 && r.height>10;"
                    + "  }catch(e){return false;}"
                    + "});"
                    + "if(!inputs.length) return 'NO_SEARCH';"
                    + // ưu tiên input gần button đang mở
                    "var btn=document.querySelector('button.inp[data-dd-open=\"1\"]');"
                    + "var best=inputs[0];"
                    + "if(btn){ var bb=btn.getBoundingClientRect();"
                    + "  inputs.sort(function(a,b){ return Math.abs(a.getBoundingClientRect().top-bb.bottom)-Math.abs(b.getBoundingClientRect().top-bb.bottom);});"
                    + "  best=inputs[0]; }"
                    + "best.focus(); best.value='';"
                    + "best.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "best.value=q;"
                    + "best.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "best.dispatchEvent(new Event('change',{bubbles:true}));"
                    + "best.dispatchEvent(new KeyboardEvent('keyup',{key:q,bubbles:true}));"
                    + "return 'TYPED:'+q;";

    private static final String PICK_NEW_OPTION_JS =
            "var before = {}; (arguments[0]||[]).forEach(function(t){before[t]=1;});"
                    + "var reject=function(t){"
                    + " t=(t||'').replace(/\\u00a0/g,' ').trim();"
                    + " if(!t||t.length>80) return true;"
                    + " if(t.indexOf('\\n')>=0) return true;"
                    + " var l=t.toLowerCase();"
                    + " if(l.indexOf('chọn')>=0 || t==='▾' || t==='—' || t==='*' || t==='✕') return true;"
                    + " if(/^([ivxlcdm]+|[0-9]+)[.)]\\s/i.test(t)) return true;"
                    + " if(l.indexOf('không tìm thấy')>=0) return true;"
                    + " if(l.indexOf('gửi biểu mẫu')>=0 || l.indexOf('tiếp theo')>=0) return true;"
                    + " if(l.indexOf('nội dung yêu cầu')>=0) return true;"
                    + " if(l.indexOf('thêm dòng')>=0 || l.indexOf('tìm kiếm')>=0) return true;"
                    + " if(l==='a' || l.length===1) return true;"
                    + " return false;"
                    + "};"
                    + "var btn=document.querySelector('button.inp[data-dd-open=\"1\"]');"
                    + "var btnRect=btn?btn.getBoundingClientRect():null;"
                    + "var walk=function(node, out){"
                    + " if(!node||node.nodeType!==1) return;"
                    + " if(node.shadowRoot) walk(node.shadowRoot, out);"
                    + " out.push(node);"
                    + " var ch=node.children||[]; for(var i=0;i<ch.length;i++) walk(ch[i], out);"
                    + "};"
                    + "var all=[]; walk(document.documentElement, all);"
                    + "var candidates=[];"
                    + "all.forEach(function(el){try{"
                    + " if(btn && (el===btn || btn.contains(el))) return;"
                    + " var st=getComputedStyle(el);"
                    + " if(st.display==='none'||st.visibility==='hidden'||Number(st.opacity)===0) return;"
                    + " var r=el.getBoundingClientRect();"
                    + " if(r.width<8||r.height<12||r.height>160) return;"
                    + " var txt=(el.innerText||el.textContent||'').trim();"
                    + " if(reject(txt)) return;"
                    + " if(before[txt]) return;"
                    + " if(el.querySelector('button.inp,input,textarea,select,label,.field,.lbl')) return;"
                    + " if(el.children && el.children.length>2) return;"
                    + " var role=(el.getAttribute('role')||'').toLowerCase();"
                    + " var cls=String(el.className||'');"
                    + " var tag=el.tagName.toLowerCase();"
                    + " var score=10;"
                    + " if(role==='option'||role==='menuitem') score+=70;"
                    + " if(st.position==='fixed'||st.position==='absolute') score+=45;"
                    + " if(/option|menu|select|dropdown|item|list|suggest/i.test(cls)) score+=30;"
                    + " if(btnRect && r.top>=btnRect.bottom-8 && r.top<=btnRect.bottom+420) score+=40;"
                    + " if(btnRect && r.left>=btnRect.left-60 && r.left<=btnRect.right+120) score+=20;"
                    + " if(tag==='li'||tag==='button') score+=12;"
                    + " candidates.push({el:el,txt:txt,score:score,top:r.top});"
                    + "}catch(e){}});"
                    + "candidates.sort(function(a,b){return b.score-a.score || a.top-b.top;});"
                    + "if(candidates.length){"
                    + "  var pick=candidates[0];"
                    + "  pick.el.dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));"
                    + "  pick.el.dispatchEvent(new MouseEvent('mouseup',{bubbles:true}));"
                    + "  pick.el.click();"
                    + "  return 'OK:'+pick.txt+'|score='+pick.score+'|n='+candidates.length;"
                    + "}"
                    + "if(btn){"
                    + "  var sel=btn.parentElement && btn.parentElement.querySelector('select');"
                    + "  if(sel){"
                    + "    for(var i=0;i<sel.options.length;i++){"
                    + "      var o=sel.options[i]; var t=(o.text||'').trim();"
                    + "      if(!t||reject(t)) continue;"
                    + "      sel.selectedIndex=i;"
                    + "      sel.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "      sel.dispatchEvent(new Event('change',{bubbles:true}));"
                    + "      return 'OK:'+t;"
                    + "    }"
                    + "  }"
                    + "}"
                    + "var abs=[]; all.forEach(function(el){try{"
                    + " var st=getComputedStyle(el); if(st.position!=='absolute'&&st.position!=='fixed') return;"
                    + " var r=el.getBoundingClientRect(); if(r.width<40||r.height<40||st.display==='none') return;"
                    + " abs.push(el.tagName+'.'+String(el.className||'').slice(0,40)+'#'+(el.innerText||'').trim().slice(0,50));"
                    + "}catch(e){}});"
                    + "return 'NONE new=0 abs='+JSON.stringify(abs.slice(0,8));";

    /** Button còn placeholder → skip; đã có giá trị → OK. */
    private static final String CONFIRM_OR_SKIP_JS =
            "var isPlaceholder=function(t){"
                    + "t=(t||'').replace(/\\u00a0/g,' ').trim().toLowerCase();"
                    + "return !t || t.indexOf('chọn')>=0 || t==='▾' || t.charAt(0)==='—' || t.charAt(0)==='-';"
                    + "};"
                    + "var btn=document.querySelector('button.inp[data-dd-open=\"1\"]')"
                    + " || [...document.querySelectorAll('button.inp')].find(b=>b.getAttribute('data-dd-open')==='1');"
                    + // nếu attribute đã bị xóa: tìm button vừa đổi gần đây không còn placeholder
                    "if(!btn){"
                    + "  var ok=[...document.querySelectorAll('button.inp')].filter(b=>!isPlaceholder(b.innerText||''));"
                    + "  return ok.length ? 'OK' : 'FAIL';"
                    + "}"
                    + "var t=btn.innerText||'';"
                    + "btn.removeAttribute('data-dd-open');"
                    + "if(isPlaceholder(t)) return 'FAIL:'+t;"
                    + "return 'OK';";

    private static final String FORCE_SKIP_OPEN_JS =
            "var b=document.querySelector('button.inp[data-dd-open=\"1\"]');"
                    + "if(b){b.setAttribute('data-skip-dd','1');b.removeAttribute('data-dd-open');}"
                    + "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));";

    private static final String KEYBOARD_SELECT_OPEN_DROPDOWN_JS =
            "var btn=document.querySelector('button.inp[data-dd-open=\"1\"]');"
                    + "if(!btn) return 'NONE';"
                    + "btn.focus();"
                    + "['ArrowDown','Enter'].forEach(function(k){"
                    + "  btn.dispatchEvent(new KeyboardEvent('keydown',{key:k,code:k,bubbles:true}));"
                    + "  document.dispatchEvent(new KeyboardEvent('keydown',{key:k,code:k,bubbles:true}));"
                    + "});"
                    + "return (btn.innerText||'').trim();";
}
