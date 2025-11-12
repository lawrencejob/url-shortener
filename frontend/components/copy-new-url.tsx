import { useCopyToClipboard } from "usehooks-ts"
import { InputGroup, InputGroupInput, InputGroupAddon, InputGroupButton } from "./ui/input-group"
import { IconCopy, IconCheck } from "@tabler/icons-react"

export function CopyNewUrl({ shortUrl }: { shortUrl: string }) {

  const [copiedText, copyToClipboard] = useCopyToClipboard()

    return <InputGroup>
        <InputGroupInput value={shortUrl} readOnly />
        <InputGroupAddon align="inline-end">
          <InputGroupButton
            aria-label="Copy"
            title="Copy"
            size="icon-xs"
            onClick={() => {
              copyToClipboard(shortUrl)
            }}
          >
            {copiedText != null ? <IconCheck /> : <IconCopy />}
          </InputGroupButton>
        </InputGroupAddon>
      </InputGroup>
}